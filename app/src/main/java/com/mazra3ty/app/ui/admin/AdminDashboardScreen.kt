package com.mazra3ty.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Job
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.database.types.Review
import com.mazra3ty.app.database.types.Application
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun AdminDashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var totalUsers       by remember { mutableStateOf(0) }
    var bannedUsers      by remember { mutableStateOf(0) }
    var totalJobs        by remember { mutableStateOf(0) }
    var openJobs         by remember { mutableStateOf(0) }
    var totalReviews     by remember { mutableStateOf(0) }
    var totalApplications by remember { mutableStateOf(0) }
    var isLoading        by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val users = SupabaseClientProvider.client
                    .postgrest["active_users"].select().decodeList<User>()
                val jobs = SupabaseClientProvider.client
                    .postgrest["jobs"].select().decodeList<Job>()
                val reviews = SupabaseClientProvider.client
                    .postgrest["reviews"].select().decodeList<Review>()
                val applications = SupabaseClientProvider.client
                    .postgrest["applications"].select().decodeList<Application>()

                totalUsers        = users.size
                bannedUsers       = users.count { it.is_banned }
                totalJobs         = jobs.size
                openJobs          = jobs.count { it.status == "open" }
                totalReviews      = reviews.size
                totalApplications = applications.size
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ── No topBar here — the shared AdminTopBar lives in AdminHome's Scaffold ──
    Scaffold(
        containerColor = Color(0xFFFFFFFF)
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

            // ── Welcome banner ─────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Welcome back ", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        Text(
                            "Administrator",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$totalUsers total users · $openJobs open jobs",
                            color    = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        Icons.Outlined.Agriculture,
                        null,
                        tint     = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // ── Stats grid ────────────────────────────────────────────────
            Text("Overview", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.People,
                    label    = "Users",
                    value    = totalUsers.toString(),
                    sub      = "$bannedUsers banned",
                    iconBg   = GreenPrimary.copy(alpha = 0.12f),
                    iconTint = GreenPrimary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.Work,
                    label    = "Jobs",
                    value    = totalJobs.toString(),
                    sub      = "$openJobs open",
                    iconBg   = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF1E88E5)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.Star,
                    label    = "Reviews",
                    value    = totalReviews.toString(),
                    sub      = "total ratings",
                    iconBg   = Color(0xFFFFF8E1),
                    iconTint = Color(0xFFFFC107)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.Assignment,
                    label    = "Applications",
                    value    = totalApplications.toString(),
                    sub      = "total",
                    iconBg   = Color(0xFFFCE4EC),
                    iconTint = Color(0xFFE91E63)
                )
            }


            }

        }
    }


// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon:     ImageVector,
    label:    String,
    value:    String,
    sub:      String,
    iconBg:   Color,
    iconTint: Color
) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(sub,   fontSize = 11.sp, color = GrayMedium)
        }
    }
}



// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun AdminDashboardPreview() {
    Mazra3tyTheme {
        AdminDashboardScreen(onNavigate = {}, onLogout = {})
    }
}