package com.mazra3ty.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onBack: () -> Unit) {

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

    // Computed stats
    val workers = users.count { it.role == "worker" }
    val farmers = users.count { it.role == "farmer" }
    val banned = users.count { it.is_banned }
    val openJobs = jobs.count { it.status == "open" }
    val closedJobs = jobs.count { it.status == "closed" }
    val avgRating = if (reviews.isEmpty()) 0f else reviews.map { it.rating }.average().toFloat()
    val fiveStars = reviews.count { it.rating == 5 }
    val pendingApps = applications.count { it.status == "pending" }
    val acceptedApps = applications.count { it.status == "accepted" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Users section ─────────────────────────────────────────────
            SectionTitle(icon = Icons.Outlined.People, title = "Users")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBadge(Modifier.weight(1f), "Total", users.size.toString(), GreenPrimary)
                StatBadge(Modifier.weight(1f), "Workers", workers.toString(), Color(0xFF1E88E5))
                StatBadge(Modifier.weight(1f), "Farmers", farmers.toString(), Color(0xFFFB8C00))
            }

            BarChartCard(
                title = "User Breakdown",
                bars = listOf(
                    BarData("Workers", workers, users.size, Color(0xFF1E88E5)),
                    BarData("Farmers", farmers, users.size, Color(0xFFFB8C00)),
                    BarData("Banned", banned, users.size, RedError),
                )
            )

            // ── Jobs section ──────────────────────────────────────────────
            SectionTitle(icon = Icons.Outlined.Work, title = "Jobs")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBadge(Modifier.weight(1f), "Total", jobs.size.toString(), GreenPrimary)
                StatBadge(Modifier.weight(1f), "Open", openJobs.toString(), Color(0xFF4CAF50))
                StatBadge(Modifier.weight(1f), "Closed", closedJobs.toString(), GrayDark)
            }

            BarChartCard(
                title = "Jobs Status",
                bars = listOf(
                    BarData("Open", openJobs, jobs.size.coerceAtLeast(1), GreenPrimaryDark),
                    BarData("Closed", closedJobs, jobs.size.coerceAtLeast(1), GrayMedium),
                )
            )

            // ── Reviews section ───────────────────────────────────────────
            SectionTitle(icon = Icons.Outlined.Star, title = "Reviews")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBadge(Modifier.weight(1f), "Total", reviews.size.toString(), GreenPrimary)
                StatBadge(Modifier.weight(1f), "Avg Rating", "%.1f ★".format(avgRating), Color(0xFFFFC107))
                StatBadge(Modifier.weight(1f), "5 Stars", fiveStars.toString(), Color(0xFFFFC107))
            }

            BarChartCard(
                title = "Ratings Distribution",
                bars = (5 downTo 1).map { star ->
                    val count = reviews.count { it.rating == star }
                    BarData(
                        label = "$star ★",
                        value = count,
                        max = reviews.size.coerceAtLeast(1),
                        color = if (star >= 4) GreenPrimary else if (star == 3) Color(0xFFFFC107) else RedError
                    )
                }
            )

            // ── Applications section ──────────────────────────────────────
            SectionTitle(icon = Icons.Outlined.Assignment, title = "Applications")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBadge(Modifier.weight(1f), "Total", applications.size.toString(), GreenPrimary)
                StatBadge(Modifier.weight(1f), "Pending", pendingApps.toString(), Color(0xFFFB8C00))
                StatBadge(Modifier.weight(1f), "Accepted", acceptedApps.toString(), GreenPrimaryDark)
            }

            BarChartCard(
                title = "Applications Status",
                bars = listOf(
                    BarData("Pending", pendingApps, applications.size.coerceAtLeast(1), Color(0xFFFB8C00)),
                    BarData("Accepted", acceptedApps, applications.size.coerceAtLeast(1), GreenPrimaryDark),
                    BarData("Other", applications.size - pendingApps - acceptedApps,
                        applications.size.coerceAtLeast(1), GrayMedium),
                )
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Section Title ────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
    }
}

// ─── Stat Badge ───────────────────────────────────────────────────────────────

@Composable
private fun StatBadge(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 11.sp, color = GrayDark)
        }
    }
}

// ─── Bar Chart Card ───────────────────────────────────────────────────────────

data class BarData(val label: String, val value: Int, val max: Int, val color: Color)

@Composable
private fun BarChartCard(title: String, bars: List<BarData>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            bars.forEach { bar ->
                val fraction = if (bar.max == 0) 0f else bar.value.toFloat() / bar.max
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        bar.label,
                        fontSize = 12.sp,
                        color = GrayDark,
                        modifier = Modifier.width(70.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(GrayLight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(50))
                                .background(bar.color)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        bar.value.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = bar.color,
                        modifier = Modifier.width(28.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}