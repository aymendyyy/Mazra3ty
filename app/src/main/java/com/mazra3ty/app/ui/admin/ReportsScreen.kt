package com.mazra3ty.app.ui.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.*
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// ─── Report Item model ────────────────────────────────────────────────────────

data class ReportItem(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
    val time: String
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit) {

    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var applications by remember { mutableStateOf<List<Application>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                users = SupabaseClientProvider.client.postgrest["users"].select().decodeList()
                jobs = SupabaseClientProvider.client.postgrest["jobs"].select().decodeList()
                reviews = SupabaseClientProvider.client.postgrest["reviews"].select().decodeList()
                applications = SupabaseClientProvider.client.postgrest["applications"].select().decodeList()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ── Recent activity feed (sorted by created_at) ───────────────────────────
    val recentItems: List<ReportItem> by remember(users, jobs, reviews, applications) {
        derivedStateOf {
            val list = mutableListOf<ReportItem>()

            users.sortedByDescending { it.created_at }.take(3).forEach { user ->
                list.add(ReportItem(
                    icon = Icons.Outlined.PersonAdd,
                    iconBg = GreenPrimary.copy(alpha = 0.1f),
                    iconTint = GreenPrimary,
                    title = "New user joined",
                    subtitle = "${user.full_name} · ${user.role}",
                    time = formatTime(user.created_at)
                ))
            }

            jobs.sortedByDescending { it.created_at }.take(3).forEach { job ->
                list.add(ReportItem(
                    icon = Icons.Outlined.Work,
                    iconBg = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF1E88E5),
                    title = "New job posted",
                    subtitle = job.title + (job.location?.let { " · $it" } ?: ""),
                    time = formatTime(job.created_at)
                ))
            }

            reviews.sortedByDescending { it.created_at }.take(3).forEach { review ->
                list.add(ReportItem(
                    icon = Icons.Outlined.Star,
                    iconBg = Color(0xFFFFF8E1),
                    iconTint = Color(0xFFFFC107),
                    title = "New review submitted",
                    subtitle = "${"★".repeat(review.rating)} ${review.comment?.take(30) ?: ""}",
                    time = formatTime(review.created_at)
                ))
            }

            applications.sortedByDescending { it.created_at }.take(3).forEach { app ->
                list.add(ReportItem(
                    icon = Icons.Outlined.Assignment,
                    iconBg = Color(0xFFFCE4EC),
                    iconTint = Color(0xFFE91E63),
                    title = "New application",
                    subtitle = "Status: ${app.status}",
                    time = formatTime(app.created_at)
                ))
            }

            list.sortedByDescending { it.time }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Summary cards ──────────────────────────────────────────────
            item {
                Text("Summary", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.People,
                        label = "Total Users",
                        value = users.size.toString(),
                        color = GreenPrimary
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Block,
                        label = "Banned",
                        value = users.count { it.is_banned }.toString(),
                        color = RedError
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Work,
                        label = "Open Jobs",
                        value = jobs.count { it.status == "open" }.toString(),
                        color = Color(0xFF1E88E5)
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CheckCircle,
                        label = "Accepted Apps",
                        value = applications.count { it.status == "accepted" }.toString(),
                        color = GreenPrimaryDark
                    )
                }
            }

            // ── Recent activity ────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Text("Recent Activity", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
            }

            if (recentItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent activity", color = GrayMedium)
                    }
                }
            } else {
                items(recentItems) { item ->
                    ActivityCard(item)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─── Summary Card ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
                Text(label, fontSize = 11.sp, color = GrayDark)
            }
        }
    }
}

// ─── Activity Card ────────────────────────────────────────────────────────────

@Composable
private fun ActivityCard(item: ReportItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(item.iconBg)
                    .border(1.dp, item.iconTint.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, tint = item.iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextPrimary)
                Text(item.subtitle, fontSize = 11.sp, color = GrayDark, maxLines = 1)
            }
            Text(item.time, fontSize = 11.sp, color = GrayMedium)
        }
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun formatTime(createdAt: String?): String {
    return try {
        val instant = Instant.parse(createdAt ?: return "—")
        val days = ChronoUnit.DAYS.between(instant, Instant.now())
        val hours = ChronoUnit.HOURS.between(instant, Instant.now())
        when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            else -> "Just now"
        }
    } catch (e: Exception) { "—" }
}