package com.mazra3ty.app.ui.farmer
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.*
import com.mazra3ty.app.ui.component.HomeTopBar
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// ─── Placeholder Sponsor model ────────────────────────────────────────────────
// (move to TableTypes.kt once the Supabase table is ready)
data class Sponsor(
    val id: String,
    val name: String,
    val tagline: String? = null,
    val iconTint: Color = GreenPrimary,
    val bgColor: Color = Color(0xFFEAF5D8),
    val is_active: Boolean = true
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerHomeScreen(
    userId: String,
    onWorkerProfile: (String) -> Unit = {},      // workerId
    onJobDetail: (String) -> Unit = {},          // jobId
    onViewAllWorkers: () -> Unit = {},
    onViewAllJobs: () -> Unit = {},
    onCreateJob: () -> Unit = {},
    onViewNotifications: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<User?>(null) }
    var workers by remember { mutableStateOf<List<User>>(emptyList()) }
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var workerProfiles by remember { mutableStateOf<Map<String, Profile>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Load current farmer and other data
    LaunchedEffect(userId) {
        scope.launch {
            try {
                // Fetch current user
                val user = SupabaseClientProvider.client
                    .postgrest["users"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingle<User>()
                currentUser = user

                // Fetch workers and profiles
                val allUsers = SupabaseClientProvider.client
                    .postgrest["users"].select().decodeList<User>()
                val allProfiles = SupabaseClientProvider.client
                    .postgrest["profiles"].select().decodeList<Profile>()
                val allJobs = SupabaseClientProvider.client
                    .postgrest["jobs"].select().decodeList<Job>()

                workers = allUsers.filter { it.role == "worker" }.take(10)
                workerProfiles = allProfiles.associateBy { it.user_id }
                jobs = allJobs.filter { it.status == "open" }.take(10)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    if (currentUser == null && isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary)
        }
        return
    }

    val user = currentUser ?: return

    val filteredWorkers = remember(workers, searchQuery) {
        if (searchQuery.isBlank()) workers
        else workers.filter { it.full_name.contains(searchQuery, ignoreCase = true) }
    }
    val filteredJobs = remember(jobs, searchQuery) {
        if (searchQuery.isBlank()) jobs
        else jobs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.location?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7F2)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Top Bar ──────────────────────────────────────────────────────
            item {
                HomeTopBar(
                    user = user,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onNotificationsClick = onViewNotifications,
                    onAddClick = onCreateJob
                )
            }

            // ── Sponsored Ads ────────────────────────────────────────────────
            item { Spacer(Modifier.height(20.dp)) }
            item {
                SectionHeader(title = "Sponsored", actionLabel = null, onAction = {})
            }
            item { Spacer(Modifier.height(10.dp)) }
            item {
                // Fetch active sponsors from database (using real Sponsor data class)
                val sponsors = remember { mutableStateOf<List<Sponsor>>(emptyList()) }
                LaunchedEffect(Unit) {
                    try {
                        val fetched = SupabaseClientProvider.client
                            .postgrest["sponsors"]
                            .select { filter { eq("is_active", true) } }
                            .decodeList<Sponsor>()
                        sponsors.value = fetched
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (sponsors.value.isNotEmpty()) {
                    SponsoredAdsRow(sponsors = sponsors.value)
                }
            }

            // ── Explored Workers ─────────────────────────────────────────────
            item { Spacer(Modifier.height(20.dp)) }
            item {
                SectionHeader(
                    title = "Explored Workers",
                    actionLabel = "see all",
                    onAction = onViewAllWorkers
                )
            }
            item { Spacer(Modifier.height(10.dp)) }

            if (isLoading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(140.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp) }
                }
            } else {
                item {
                    WorkersRow(
                        workers = filteredWorkers,
                        profiles = workerProfiles,
                        onWorkerClick = onWorkerProfile
                    )
                }
            }

            // ── Recent Job Offers ─────────────────────────────────────────────
            item { Spacer(Modifier.height(20.dp)) }
            item {
                SectionHeader(
                    title = "Recent posts",
                    actionLabel = "see all",
                    onAction = onViewAllJobs
                )
            }
            item { Spacer(Modifier.height(10.dp)) }

            if (isLoading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp) }
                }
            } else if (filteredJobs.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No open jobs right now", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(filteredJobs, key = { it.id }) { job ->
                    JobOfferCard(
                        job = job,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                        onClick = { onJobDetail(job.id) }
                    )
                }
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null) {
            Row(
                modifier = Modifier.clickable { onAction() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(actionLabel, fontSize = 13.sp, color = GreenPrimaryDark, fontWeight = FontWeight.Medium)
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = GreenPrimaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Sponsored Ads ────────────────────────────────────────────────────────────

@Composable
private fun SponsoredAdsRow(sponsors: List<Sponsor>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sponsors, key = { it.id }) { sponsor ->
            SponsorCard(sponsor = sponsor)
        }
    }
}

@Composable
private fun SponsorCard(sponsor: Sponsor) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF5D8)), // fallback
        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.25f)),
        modifier = Modifier
            .width(200.dp)
            .height(108.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background decorative circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary.copy(alpha = 0.08f))
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GreenPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "SPONSOR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Column {
                    Text(
                        text = sponsor.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    sponsor.tagline?.let {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = GrayDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
// ─── Workers Row ─────────────────────────────────────────────────────────────

@Composable
private fun WorkersRow(
    workers: List<User>,
    profiles: Map<String, Profile>,
    onWorkerClick: (String) -> Unit
) {
    if (workers.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No workers found", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(workers, key = { it.id }) { worker ->
            WorkerCard(
                worker = worker,
                profile = profiles[worker.id],
                onClick = { onWorkerClick(worker.id) }
            )
        }
    }
}

@Composable
private fun WorkerCard(
    worker: User,
    profile: Profile?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.2f)),
        modifier = Modifier
            .width(138.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary.copy(alpha = 0.12f))
                    .border(2.dp, GreenPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = worker.full_name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            profile?.skills?.let { skills ->
                Spacer(Modifier.height(3.dp))
                Text(
                    text = skills,
                    fontSize = 11.sp,
                    color = GrayMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            profile?.location?.let { loc ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = GrayMedium,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(loc, fontSize = 10.sp, color = GrayMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Rating placeholder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text("4.5", fontSize = 11.sp, color = Color(0xFFFFC107), fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─── Job Offer Card ───────────────────────────────────────────────────────────

@Composable
fun JobOfferCard(
    job: Job,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Agriculture,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    job.location?.let { loc ->
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = GrayMedium,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(loc, fontSize = 12.sp, color = GrayMedium, maxLines = 1)
                        Spacer(Modifier.width(10.dp))
                    }
                    // Status chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (job.status == "open") Color(0xFFE8F5E9)
                                else Color(0xFFFCE4EC)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = job.status.replaceFirstChar { it.uppercase() },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (job.status == "open") Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                job.salary?.let { salary ->
                    Text(
                        text = "${salary.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GreenPrimaryDark
                    )
                    Text("DZD/J", fontSize = 10.sp, color = GrayMedium)
                }
            }
        }
    }
}
