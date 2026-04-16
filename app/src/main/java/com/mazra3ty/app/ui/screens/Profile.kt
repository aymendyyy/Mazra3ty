package com.mazra3ty.app.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Profile
import com.mazra3ty.app.database.types.Review
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.database.types.WorkerPost
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var user     by remember { mutableStateOf<User?>(null) }
    var profile  by remember { mutableStateOf<Profile?>(null) }
    var reviews  by remember { mutableStateOf<List<Review>>(emptyList()) }
    var posts    by remember { mutableStateOf<List<WorkerPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        scope.launch {
            try {
                user = SupabaseClientProvider.client
                    .postgrest["users"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingle()

                profile = runCatching {
                    SupabaseClientProvider.client
                        .postgrest["profiles"]
                        .select { filter { eq("user_id", userId) } }
                        .decodeSingle<Profile>()
                }.getOrNull()

                reviews = SupabaseClientProvider.client
                    .postgrest["reviews"]
                    .select { filter { eq("reviewed_id", userId) } }
                    .decodeList()

                if (user?.role == "worker") {
                    posts = SupabaseClientProvider.client
                        .postgrest["worker_posts"]
                        .select { filter { eq("worker_id", userId) } }
                        .decodeList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return@Scaffold
        }

        val currentUser = user ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("User not found", color = Color.Gray)
            }
            return@Scaffold
        }

        val avgRating = if (reviews.isEmpty()) 0f
        else reviews.map { it.rating }.average().toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header (Cover + Avatar) ────────────────────────────────────
            ProfileHeader(
                user        = currentUser,
                profile     = profile,
                avgRating   = avgRating,
                reviewCount = reviews.size,
                padding     = padding
            )

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Bio ───────────────────────────────────────────────────
                currentUser.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                    SectionCard(title = "About") {
                        Text(
                            text  = bio,
                            fontSize = 14.sp,
                            color = Color(0xFF444444),
                            lineHeight = 22.sp
                        )
                    }
                }

                // ── Info ──────────────────────────────────────────────────
                SectionCard(title = "Information") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentUser.email?.let {
                            InfoRow(Icons.Outlined.Email, "Email", it)
                        }
                        currentUser.phone?.let {
                            InfoRow(Icons.Outlined.Phone, "Phone", it)
                        }
                        profile?.location?.let {
                            InfoRow(Icons.Outlined.LocationOn, "Location", it)
                        }
                        currentUser.date_of_birth?.let {
                            InfoRow(Icons.Outlined.Cake, "Date of Birth", it)
                        }
                        currentUser.created_at?.let {
                            val joined = it.take(10) // "YYYY-MM-DD"
                            InfoRow(Icons.Outlined.CalendarToday, "Joined", joined)
                        }
                        InfoRow(
                            icon  = Icons.Outlined.Badge,
                            label = "Role",
                            value = currentUser.role.replaceFirstChar { it.uppercase() }
                        )
                    }
                }

                // ── Skills ────────────────────────────────────────────────
                profile?.skills?.takeIf { it.isNotBlank() }?.let { skills ->
                    SectionCard(title = "Skills") {
                        SkillsChips(skills)
                    }
                }

                // ── Stats ─────────────────────────────────────────────────
                SectionCard(title = "Stats") {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatItem(value = reviews.size.toString(), label = "Reviews")
                        VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFEEEEEE))
                        StatItem(
                            value = if (avgRating > 0) "%.1f".format(avgRating) else "—",
                            label = "Avg Rating"
                        )
                        VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFEEEEEE))
                        StatItem(value = posts.size.toString(), label = "Posts")
                    }
                }

                // ── Recent Reviews ────────────────────────────────────────
                if (reviews.isNotEmpty()) {
                    SectionCard(title = "Reviews (${reviews.size})") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            reviews.take(3).forEach { review ->
                                ReviewItem(review)
                            }
                        }
                    }
                }
                // ── Worker Posts ──────────────────────────────────────────
                if (posts.isNotEmpty()) {
                    SectionCard(title = "Posts (${posts.size})") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            posts.take(3).forEach { post ->
                                WorkerPostItem(post)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─── Profile Header ───────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    user: User,
    profile: Profile?,
    avgRating: Float,
    reviewCount: Int,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding())
    ) {
        // Cover image / gradient banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4CAF50),
                            Color(0xFF8BC34A),
                            Color(0xFFA5D66D)
                        )
                    )
                )
        ) {
            // Subtle pattern overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.08f))
            )
            Icon(
                imageVector = Icons.Outlined.Agriculture,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
            )
        }

        // Avatar + name block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(140.dp)) // push avatar to overlap banner

            // Avatar with badge
            Box(modifier = Modifier.size(90.dp)) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.full_name.take(1).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimaryDark
                        )
                    }
                }
                // Verified badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF1DA1F2))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Name
            Text(
                text = user.full_name,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextPrimary
            )

            Spacer(Modifier.height(2.dp))

            // Location
            profile?.location?.let { loc ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = GrayMedium, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(loc, fontSize = 13.sp, color = GrayDark)
                }
                Spacer(Modifier.height(4.dp))
            }

            // Role chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GreenPrimary.copy(alpha = 0.12f),
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = user.role.replaceFirstChar { it.uppercase() },
                    color = GreenPrimaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Rating row
            if (reviewCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "%.1f".format(avgRating),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "($reviewCount Avis)",
                        fontSize = 13.sp,
                        color = GrayMedium
                    )
                }
            }

            // Ban badge (admin view)
            if (user.is_banned) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RedError.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Block, null, tint = RedError, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Banned", color = RedError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Reusable Components ──────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GreenPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = GrayMedium)
            Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = GreenPrimaryDark)
        Text(label, fontSize = 12.sp, color = GrayMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsChips(skills: String) {
    val list = skills.split(",").map { it.trim() }.filter { it.isNotBlank() }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        list.forEach { skill ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SoftGreenBg
            ) {
                Text(
                    text = skill,
                    fontSize = 12.sp,
                    color = GreenPrimaryDark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8F8F8))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GreenPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    Icon(
                        imageVector = if (i < review.rating) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                review.created_at?.let {
                    Text(it.take(10), fontSize = 11.sp, color = GrayMedium)
                }
            }
            review.comment?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, fontSize = 13.sp, color = Color(0xFF444444), lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun WorkerPostItem(post: WorkerPost) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8F8F8))
            .padding(12.dp)
    ) {
        post.title?.takeIf { it.isNotBlank() }?.let {
            Text(it, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = post.description,
            fontSize = 13.sp,
            color = Color(0xFF555555),
            lineHeight = 18.sp,
            maxLines = 2
        )
        post.skills?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Build, null, tint = GreenPrimary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(it, fontSize = 12.sp, color = GreenPrimaryDark)
            }
        }
        post.location?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = GrayMedium, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(it, fontSize = 12.sp, color = GrayDark)
            }
        }
    }
}

