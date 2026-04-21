package com.mazra3ty.app.ui.farmer
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Review
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.database.types.WorkerPost
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// ─── Data holder ──────────────────────────────────────────────────────────────

private data class WorkerItem(
    val post: WorkerPost,
    val user: User,
    val avgRating: Float,
    val reviewCount: Int
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkersPage(
    currentFarmerId: String,          // logged-in farmer's id
    onBack: () -> Unit = {},
    onSendOffer: (workerPost: WorkerPost, worker: User) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()

    var workers  by remember { mutableStateOf<List<WorkerItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterAvailable by remember { mutableStateOf<Boolean?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // ── Offer dialog state ────────────────────────────────────────────────────
    var offerTarget by remember { mutableStateOf<WorkerItem?>(null) }
    var offerMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var offerSent by remember { mutableStateOf(false) }

    // ── Load data ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        try {
            val posts = SupabaseClientProvider.client
                .postgrest["worker_posts"]
                .select { limit(20) }
                .decodeList<WorkerPost>()

            val workerIds = posts.map { it.worker_id }

            val users = SupabaseClientProvider.client
                .postgrest["users"]
                .select {
                    filter { isIn("id", workerIds) }
                }
                .decodeList<User>()
                .associateBy { it.id }

            val reviews = SupabaseClientProvider.client
                .postgrest["reviews"]
                .select {
                    filter { isIn("reviewed_id", workerIds) }
                }
                .decodeList<Review>()

            val reviewsByWorker = reviews.groupBy { it.reviewed_id }

            workers = posts.mapNotNull { post ->
                val user = users[post.worker_id] ?: return@mapNotNull null
                val workerReviews = reviewsByWorker[post.worker_id] ?: emptyList()
                val avg = if (workerReviews.isEmpty()) 0f
                else workerReviews.map { it.rating }.average().toFloat()

                WorkerItem(post, user, avg, workerReviews.size)
            }

        } catch (e: Exception) {
            Log.e("WorkersPage", "Error loading workers", e)
        } finally {
            isLoading = false
        }
    }
    // ── Filter ────────────────────────────────────────────────────────────────
    val filtered = workers.filter { item ->
        val q = searchQuery.trim()
        val matchSearch = q.isEmpty() ||
                item.user.full_name.contains(q, ignoreCase = true) ||
                item.post.location?.contains(q, ignoreCase = true) == true

        matchSearch
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Find Workers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "${filtered.size} available",
                            fontSize = 12.sp,
                            color = GreenPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Search + filter bar ───────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, skill, location…", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, null, tint = Color.Gray)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, null, tint = Color.Gray)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedContainerColor = Color(0xFFEEF7E8),
                        unfocusedContainerColor = Color(0xFFEEF7E8)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                Spacer(Modifier.width(10.dp))

                Box {
                    val hasFilter = filterAvailable != null
                    IconButton(
                        onClick = { showFilterMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasFilter) GreenPrimary else Color(0xFFEEF7E8)
                            )
                            .border(
                                1.dp,
                                if (hasFilter) GreenPrimary else GreenPrimary.copy(alpha = 0.4f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter",
                            tint = if (hasFilter) Color.White else GreenPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        listOf(
                            null  to "All Workers",
                            true  to "✅  Available",
                            false to "🔴  Unavailable"
                        ).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    filterAvailable = value
                                    showFilterMenu = false
                                },
                                leadingIcon = if (filterAvailable == value) {
                                    { Icon(Icons.Outlined.Check, null, tint = GreenPrimary, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Content ───────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                }

                filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.PersonSearch,
                                null,
                                tint = GrayMedium,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No workers found", color = GrayMedium, fontSize = 15.sp)
                            if (searchQuery.isNotEmpty() || filterAvailable != null) {
                                TextButton(onClick = {
                                    searchQuery = ""
                                    filterAvailable = null
                                }) {
                                    Text("Clear filters", color = GreenPrimary)
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(filtered, key = { it.post.id }) { item ->
                            WorkerCard(
                                item = item,
                                onRequestWorker = {
                                    offerTarget = item
                                    offerMessage = ""
                                    offerSent = false
                                }
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    // ── Offer Dialog ──────────────────────────────────────────────────────────
    offerTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                if (!isSending) {
                    offerTarget = null
                    offerMessage = ""
                    offerSent = false
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                if (offerSent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = GreenPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Offer Sent!", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Send, null, tint = GreenPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Send Job Offer", fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                if (offerSent) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your offer was sent to ${target.user.full_name} successfully.",
                            color = GrayDark,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column {
                        // Worker mini-header inside dialog
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GreenPrimary.copy(alpha = 0.15f))
                                    .border(1.5.dp, GreenPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = target.user.full_name.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = GreenPrimary
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    target.user.full_name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                target.post.location?.let {
                                    Text(it, fontSize = 12.sp, color = GrayMedium)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = offerMessage,
                            onValueChange = { offerMessage = it },
                            placeholder = {
                                Text(
                                    "Add a message (optional)…",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                focusedContainerColor = Color(0xFFEEF7E8),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (offerSent) {
                    Button(
                        onClick = {
                            offerTarget = null
                            offerSent = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            isSending = true
                            scope.launch {
                                try {
                                    onSendOffer(target.post, target.user)
                                    offerSent = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isSending = false
                                }
                            }
                        },
                        enabled = !isSending,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Outlined.Send, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Send Offer", color = Color.White)
                    }
                }
            },
            dismissButton = {
                if (!offerSent) {
                    TextButton(
                        onClick = { if (!isSending) { offerTarget = null; offerMessage = "" } }
                    ) {
                        Text("Cancel", color = GrayDark)
                    }
                }
            }
        )
    }
}

// ─── Worker Card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkerCard(
    item: WorkerItem,
    onRequestWorker: () -> Unit
) {
    val isAvailable = item.post.status.toString().lowercase() == "active"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            GreenPrimary.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {

            // ── Gradient accent strip ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = if (isAvailable)
                                listOf(GreenPrimary, GreenLight)
                            else
                                listOf(GrayMedium, GrayLight)
                        )
                    )
            )

            Column(modifier = Modifier.padding(14.dp)) {

                // ── Header ────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar with initial
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GreenPrimary.copy(alpha = 0.25f),
                                        GreenPrimary.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(2.dp, GreenPrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.user.full_name
                                .trim()
                                .split(" ")
                                .take(2)
                                .joinToString("") { it.firstOrNull()?.uppercase() ?: "" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GreenPrimaryDark
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Name
                        Text(
                            text = item.user.full_name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(3.dp))

                        // Rating row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i ->
                                Icon(
                                    imageVector = when {
                                        i < item.avgRating.toInt() -> Icons.Outlined.Star
                                        i < item.avgRating         -> Icons.Outlined.StarHalf
                                        else                       -> Icons.Outlined.StarOutline
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (item.avgRating > 0f)
                                    "${"%.1f".format(item.avgRating)} (${item.reviewCount})"
                                else "No reviews yet",
                                fontSize = 11.sp,
                                color = GrayMedium
                            )
                        }

                        Spacer(Modifier.height(3.dp))

                        // Location
                        item.post.location?.let { loc ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    null,
                                    tint = GrayMedium,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = loc,
                                    fontSize = 12.sp,
                                    color = GrayMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Availability badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAvailable)
                            Color(0xFFE8F5E9)
                        else
                            Color(0xFFFFEBEE),
                        border = BorderStroke(
                            1.dp,
                            if (isAvailable) Color(0xFF81C784) else Color(0xFFEF9A9A)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isAvailable) Color(0xFF4CAF50) else Color(0xFFEF5350)
                                    )
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (isAvailable) "Available" else "Unavailable",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(10.dp))

                // ── Body ──────────────────────────────────────────────────

                // Post title
                if (item.post.title.isNotBlank()) {
                    val title = item.post.title
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // Bio / description
                Text(
                    text = item.post.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 3,
                    lineHeight = 19.sp,
                    overflow = TextOverflow.Ellipsis
                )

                // Skills chips
                val skills = item.post.skills
                if (!skills.isNullOrEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Construction,
                            null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Skills",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenPrimaryDark
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Column {
                        skills.chunked(3).forEach { rowSkills ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowSkills.forEach { skill ->
                                    SkillChip(skill)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // ── Request Button ────────────────────────────────────────
                Button(
                    onClick = onRequestWorker,
                    enabled = isAvailable,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary,
                        disabledContainerColor = GrayLight
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Send,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isAvailable) Color.White else GrayMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isAvailable) "Request Worker" else "Currently Unavailable",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isAvailable) Color.White else GrayMedium
                    )
                }
            }
        }
    }
}

// ─── Skill Chip ──────────────────────────────────────────────────────────────

@Composable
private fun SkillChip(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SoftGreenBg,
        border = BorderStroke(
            0.5.dp, GreenPrimary.copy(alpha = 0.35f)
        )
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GreenPrimaryDark,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}