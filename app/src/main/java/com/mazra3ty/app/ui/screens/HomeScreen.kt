package com.mazra3ty.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mazra3ty.app.ui.theme.*
import com.mazra3ty.app.ui.admin.*

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — role-aware entry point
// Call site: HomeScreen(userEmail, userRole, onLogout)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    userEmail: String,
    userRole: String,           // already resolved at auth time — no extra fetch needed
    onLogout: () -> Unit
) {
    when (userRole) {
        "admin"  -> AdminHome(userEmail = userEmail, onLogout = onLogout)
        "farmer" -> FarmerHome(userEmail = userEmail, onLogout = onLogout)
        else     -> WorkerHome(userEmail = userEmail, onLogout = onLogout)   // "worker" + fallback
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Admin layout — BottomBar + screen routing
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AdminHome(
    userEmail: String,
    onLogout: () -> Unit
) {
    // Single source of truth for current admin route
    var currentRoute by remember { mutableStateOf(AdminScreen.Dashboard.route) }

    AdminGuard(onUnauthorized = onLogout) {
        Scaffold(
            containerColor = Color(0xFFF5F5F5),
            bottomBar = {
                AdminBottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { route -> currentRoute = route }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentRoute) {
                    AdminScreen.Dashboard.route ->
                        AdminDashboardScreen(
                            onNavigate = { route -> currentRoute = route },
                            onLogout   = onLogout
                        )

                    AdminScreen.Users.route ->
                        UsersManagementScreen(
                            onBack = { currentRoute = AdminScreen.Dashboard.route }
                        )

                    AdminScreen.Ads.route ->
                        AdsMonitorScreen(
                            onBack = { currentRoute = AdminScreen.Dashboard.route }
                        )

                    AdminScreen.Reviews.route ->
                        ReviewsMonitorScreen(
                            onBack = { currentRoute = AdminScreen.Dashboard.route }
                        )

                    AdminScreen.Statistics.route ->
                        StatisticsScreen(
                            onBack = { currentRoute = AdminScreen.Dashboard.route }
                        )

                    AdminScreen.Reports.route ->
                        ReportsScreen(
                            onBack = { currentRoute = AdminScreen.Dashboard.route }
                        )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Farmer layout  (replace body with real FarmerDashboard when ready)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FarmerHome(
    userEmail: String,
    onLogout: () -> Unit
) {
    // TODO: replace with FarmerDashboard(userEmail, onLogout)
    RolePlaceholderScreen(
        role     = "Farmer 🌾",
        email    = userEmail,
        onLogout = onLogout
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Worker layout  (replace body with real WorkerDashboard when ready)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkerHome(
    userEmail: String,
    onLogout: () -> Unit
) {
    // TODO: replace with WorkerDashboard(userEmail, onLogout)
    RolePlaceholderScreen(
        role     = "Worker 👷",
        email    = userEmail,
        onLogout = onLogout
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared placeholder UI (used until Farmer/Worker screens are built)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RolePlaceholderScreen(
    role: String,
    email: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SoftGreenBg, Color(0xFFEAF3DA))))
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = GreenPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Welcome $role", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(12.dp))

        Text(email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

        Spacer(Modifier.height(30.dp))

        // Profile card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Your Profile")
                    Text(
                        "Manage your account",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Logout
        Button(
            onClick   = onLogout,
            modifier  = Modifier.fillMaxWidth(),
            colors    = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Icon(Icons.Outlined.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}