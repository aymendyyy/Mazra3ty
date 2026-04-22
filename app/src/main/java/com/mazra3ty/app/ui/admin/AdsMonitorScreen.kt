package com.mazra3ty.app.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Job
import com.mazra3ty.app.database.types.UserWithProfile
import com.mazra3ty.app.database.types.WorkerPost
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.GreenPrimaryDark
import com.mazra3ty.app.ui.theme.RedError
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// ─── Content tab ─────────────────────────────────────────────────────────────

private enum class AdsTab { JOBS, WORKER_POSTS }

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdsMonitorScreen(onBack: () -> Unit) {

    val scope = rememberCoroutineScope()

    // ── Data ──────────────────────────────────────────────────────────────────
    var jobs        by remember { mutableStateOf<List<Job>>(emptyList()) }
    var workerPosts by remember { mutableStateOf<List<WorkerPost>>(emptyList()) }
    var usersMap    by remember { mutableStateOf<Map<String, UserWithProfile>>(emptyMap()) }
    var isLoading   by remember { mutableStateOf(true) }

    // ── UI state ──────────────────────────────────────────────────────────────
    var activeTab          by remember { mutableStateOf(AdsTab.JOBS) }
    var searchQuery        by remember { mutableStateOf("") }
    var filterStatus       by remember { mutableStateOf<String?>(null) }
    var showFilterMenu     by remember { mutableStateOf(false) }
    var expandedJobId      by remember { mutableStateOf<String?>(null) }
    var expandedPostId     by remember { mutableStateOf<String?>(null) }
    var jobToDelete        by remember { mutableStateOf<Job?>(null) }
    var workerPostToDelete by remember { mutableStateOf<WorkerPost?>(null) }

    // ── Load ──────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                jobs        = SupabaseClientProvider.client.postgrest["jobs"].select().decodeList()
                workerPosts = SupabaseClientProvider.client.postgrest["worker_posts"].select().decodeList()

                // Query the flat view – no join tricks needed
                val userList: List<UserWithProfile> =
                    SupabaseClientProvider.client
                        .postgrest["user_with_profile"]
                        .select()
                        .decodeList()

                usersMap = userList.associateBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ── Filtered lists ────────────────────────────────────────────────────────
    val filteredJobs = jobs.filter { job ->
        val q = job.title.contains(searchQuery, ignoreCase = true) ||
                job.location?.contains(searchQuery, ignoreCase = true) == true
        val s = filterStatus == null || job.status == filterStatus
        q && s
    }

    val filteredPosts = workerPosts.filter { post ->
        searchQuery.isBlank() ||
                post.title?.contains(searchQuery, ignoreCase = true) == true ||
                post.description.contains(searchQuery, ignoreCase = true) ||
                post.location?.contains(searchQuery, ignoreCase = true) == true
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Ads", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
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

            // Tab chips
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdsTab.entries.forEach { tab ->
                    val sel = activeTab == tab
                    val (icon, lbl) = when (tab) {
                        AdsTab.JOBS         -> Icons.Outlined.Work        to "Job Posts"
                        AdsTab.WORKER_POSTS -> Icons.Outlined.Agriculture to "Worker Posts"
                    }
                    FilterChip(
                        selected = sel,
                        onClick  = { activeTab = tab; searchQuery = ""; filterStatus = null },
                        label    = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(icon, null, modifier = Modifier.size(14.dp))
                                Text(lbl, fontSize = 13.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor     = Color.White,
                            containerColor         = Color.White,
                            labelColor             = Color.DarkGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = sel,
                            selectedBorderColor = GreenPrimary,
                            borderColor         = Color(0xFFDDDDDD)
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search + filter
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value           = searchQuery,
                    onValueChange   = { searchQuery = it },
                    placeholder     = { Text("Search..", color = Color.Gray) },
                    leadingIcon     = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    shape           = RoundedCornerShape(24.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = GreenPrimary,
                        unfocusedBorderColor    = Color(0xFFDDDDDD),
                        focusedContainerColor   = Color(0xFFEEF7E8),
                        unfocusedContainerColor = Color(0xFFEEF7E8)
                    ),
                    singleLine = true,
                    modifier   = Modifier.weight(1f).height(52.dp)
                )
                if (activeTab == AdsTab.JOBS) {
                    Spacer(Modifier.width(10.dp))
                    Box {
                        IconButton(
                            onClick  = { showFilterMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF7E8))
                                .border(1.dp, GreenPrimary, CircleShape)
                        ) { Icon(Icons.Outlined.Tune, "Filter", tint = GreenPrimary) }
                        DropdownMenu(
                            expanded          = showFilterMenu,
                            onDismissRequest  = { showFilterMenu = false }
                        ) {
                            listOf(null to "All", "open" to "Open", "closed" to "Closed")
                                .forEach { (s, l) ->
                                    DropdownMenuItem(
                                        text    = { Text(l) },
                                        onClick = { filterStatus = s; showFilterMenu = false }
                                    )
                                }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }

                activeTab == AdsTab.JOBS -> {
                    if (filteredJobs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No job posts found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredJobs, key = { it.id }) { job ->
                                JobCard(
                                    job        = job,
                                    publisher  = usersMap[job.farmer_id],
                                    isExpanded = expandedJobId == job.id,
                                    onToggle   = {
                                        expandedJobId = if (expandedJobId == job.id) null else job.id
                                    },
                                    onDelete   = { jobToDelete = job }
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }

                else -> {
                    if (filteredPosts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No worker posts found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredPosts, key = { it.id }) { post ->
                                WorkerPostCard(
                                    post       = post,
                                    publisher  = usersMap[post.worker_id],
                                    isExpanded = expandedPostId == post.id,
                                    onToggle   = {
                                        expandedPostId = if (expandedPostId == post.id) null else post.id
                                    },
                                    onDelete   = { workerPostToDelete = post }
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    // ── Delete dialogs ────────────────────────────────────────────────────────
    jobToDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            title            = { Text("Delete Job") },
            text             = { Text("Permanently delete \"${job.title}\"? This cannot be undone.") },
            confirmButton    = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            SupabaseClientProvider.client.postgrest["jobs"]
                                .delete { filter { eq("id", job.id) } }
                            jobs = jobs.filter { it.id != job.id }
                            if (expandedJobId == job.id) expandedJobId = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            jobToDelete = null
                        }
                    }
                }) { Text("Delete", color = RedError) }
            },
            dismissButton = {
                TextButton(onClick = { jobToDelete = null }) { Text("Cancel") }
            }
        )
    }

    workerPostToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { workerPostToDelete = null },
            title            = { Text("Delete Worker Post") },
            text             = {
                Text("Permanently delete \"${post.title ?: post.description.take(40)}\"? This cannot be undone.")
            },
            confirmButton    = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            SupabaseClientProvider.client.postgrest["worker_posts"]
                                .delete { filter { eq("id", post.id) } }
                            workerPosts = workerPosts.filter { it.id != post.id }
                            if (expandedPostId == post.id) expandedPostId = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            workerPostToDelete = null
                        }
                    }
                }) { Text("Delete", color = RedError) }
            },
            dismissButton = {
                TextButton(onClick = { workerPostToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── Job Card ─────────────────────────────────────────────────────────────────

@Composable
private fun JobCard(
    job: Job,
    publisher: UserWithProfile?,          // ← fixed type
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val timeLabel   = remember(job.created_at) { adsFormatTime(job.created_at) }
    val statusColor = if (job.status == "open") GreenPrimaryDark else Color(0xFF9E9E9E)

    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Column(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp).clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.15f))
                            .border(2.dp, GreenPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Work, null, tint = GreenPrimary, modifier = Modifier.size(24.dp)) }

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(job.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        job.salary?.let {
                            Text(
                                "${it.toInt()} DZD/J",
                                fontSize = 12.sp, color = GreenPrimaryDark, fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                        Text(
                            job.status.replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        null, tint = Color.Gray, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    IconButton(
                        onClick  = onDelete,
                        modifier = Modifier
                            .size(34.dp).clip(CircleShape)
                            .background(RedError.copy(alpha = 0.12f))
                    ) { Icon(Icons.Outlined.Delete, null, tint = RedError, modifier = Modifier.size(18.dp)) }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        job.location?.let { AdsMetaRow(Icons.Outlined.LocationOn, it) }
                        AdsMetaRow(Icons.Outlined.Person, publisher?.full_name ?: "Unknown")
                    }
                    AdsMetaRow(Icons.Outlined.AccessTime, timeLabel)
                }
            }

            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                PublisherBanner(
                    sectionLabel = "Farmer / Publisher",
                    accentColor  = GreenPrimary,
                    user         = publisher,
                    postExtras   = buildList {
                        add(Icons.Outlined.Work to ("Job Status" to job.status.replaceFirstChar { it.uppercase() }))
                        job.location?.let { add(Icons.Outlined.LocationOn to ("Location" to it)) }
                        job.salary?.let   { add(Icons.Outlined.Payments   to ("Salary"   to "${it.toInt()} DZD/J")) }
                        add(Icons.Outlined.CalendarMonth to ("Posted" to timeLabel))
                    }
                )
            }
        }
    }
}

// ─── Worker Post Card ─────────────────────────────────────────────────────────

@Composable
private fun WorkerPostCard(
    post: WorkerPost,
    publisher: UserWithProfile?,          // ← fixed type
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val timeLabel = remember(post.created_at) { adsFormatTime(post.created_at) }
    val accent    = Color(0xFF1E88E5)

    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Column(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp).clip(CircleShape)
                            .background(accent.copy(alpha = 0.12f))
                            .border(2.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Agriculture, null, tint = accent, modifier = Modifier.size(24.dp)) }

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(post.title ?: "Worker Post", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(publisher?.full_name ?: "Unknown worker", fontSize = 12.sp, color = accent)
                    }

                    Icon(
                        if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        null, tint = Color.Gray, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    IconButton(
                        onClick  = onDelete,
                        modifier = Modifier
                            .size(34.dp).clip(CircleShape)
                            .background(RedError.copy(alpha = 0.12f))
                    ) { Icon(Icons.Outlined.Delete, null, tint = RedError, modifier = Modifier.size(18.dp)) }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

                Text(
                    post.description,
                    fontSize = 12.sp, color = Color.DarkGray, maxLines = 2, lineHeight = 18.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    post.location?.let { AdsInfoChip(Icons.Outlined.LocationOn, it) }
                    Spacer(Modifier.weight(1f))
                    AdsMetaRow(Icons.Outlined.AccessTime, timeLabel)
                }
            }

            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                PublisherBanner(
                    sectionLabel = "Worker / Publisher",
                    accentColor  = accent,
                    user         = publisher,
                    postExtras   = buildList {
                        post.location?.let { add(Icons.Outlined.LocationOn to ("Location" to it)) }
                        add(Icons.Outlined.CalendarMonth to ("Posted" to timeLabel))
                    }
                )
            }
        }
    }
}

// ─── Expandable publisher banner ──────────────────────────────────────────────

@Composable
private fun PublisherBanner(
    sectionLabel: String,
    accentColor:  Color,
    user: UserWithProfile?,               // ← correct type, no email field
    postExtras:   List<Pair<ImageVector, Pair<String, String>>>
) {
    Surface(
        color    = accentColor.copy(alpha = 0.06f),
        shape    = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccountCircle, null,
                    tint = accentColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(sectionLabel, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, color = accentColor)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color    = accentColor.copy(alpha = 0.15f)
            )

            if (user == null) {
                Text("Publisher data not found", fontSize = 12.sp, color = Color.Gray)
            } else {
                AdsBannerRow(Icons.Outlined.Person,        "Name",          user.full_name)
                user.phone?.let         { AdsBannerRow(Icons.Outlined.Phone,         "Phone",         it) }
                AdsBannerRow(Icons.Outlined.Badge,         "Role",          user.role.replaceFirstChar { it.uppercase() })
                user.date_of_birth?.let { AdsBannerRow(Icons.Outlined.Cake,          "Date of birth", it) }
                user.location?.let      { AdsBannerRow(Icons.Outlined.LocationOn,    "Location",      it) }
                user.bio?.let           { AdsBannerRow(Icons.Outlined.Notes,         "Bio",           it) }
                user.created_at?.let    { AdsBannerRow(Icons.Outlined.CalendarMonth, "Member since",  it.take(10)) }

                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdsBadge(
                        label = if (user.is_banned) "Banned" else "Active",
                        color = if (user.is_banned) RedError else GreenPrimaryDark
                    )
                }

                if (postExtras.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = accentColor.copy(alpha = 0.12f))
                    Spacer(Modifier.height(8.dp))
                    Text("Post Details", fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, color = accentColor)
                    Spacer(Modifier.height(4.dp))
                    postExtras.forEach { (icon, pair) ->
                        AdsBannerRow(icon, pair.first, pair.second)
                    }
                }
            }
        }
    }
}

// ─── Mini components ──────────────────────────────────────────────────────────

@Composable
private fun AdsBannerRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text("$label: ", fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AdsBadge(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AdsMetaRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun AdsInfoChip(icon: ImageVector, label: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFEEF7E8)) {
        Row(
            modifier              = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, tint = GreenPrimaryDark, modifier = Modifier.size(11.dp))
            Text(label, fontSize = 11.sp, color = GreenPrimaryDark)
        }
    }
}

// ─── Timestamp helper ─────────────────────────────────────────────────────────

private fun adsFormatTime(raw: String?): String = try {
    val instant = Instant.parse(raw ?: return "—")
    val days  = ChronoUnit.DAYS.between(instant, Instant.now())
    val hours = ChronoUnit.HOURS.between(instant, Instant.now())
    val mins  = ChronoUnit.MINUTES.between(instant, Instant.now())
    when {
        days  > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        mins  > 0 -> "${mins}m ago"
        else      -> "Just now"
    }
} catch (e: Exception) { "—" }