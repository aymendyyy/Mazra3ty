package com.mazra3ty.app.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.Mazra3tyTheme

// ─── Admin screen definitions ─────────────────────────────────────────────────

sealed class AdminScreen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard    : AdminScreen("admin_dashboard",    "Dashboard",  Icons.Outlined.Dashboard)
    object Users        : AdminScreen("admin_users",        "Users",      Icons.Outlined.People)
    object Ads          : AdminScreen("admin_ads",          "Jobs",       Icons.Outlined.Work)
    object Reviews      : AdminScreen("admin_reviews",      "Reviews",    Icons.Outlined.RateReview)
    object Reports      : AdminScreen("admin_reports",      "Reports",    Icons.Outlined.Description)
    object Statistics   : AdminScreen("admin_statistics",   "Stats",      Icons.Outlined.BarChart)
    // Applications is accessible from Dashboard quick-access & notifications
    object Applications : AdminScreen("admin_applications", "Apps",       Icons.Outlined.Assignment)
}

// Only show 5 items in the bottom bar (Applications accessible via other routes)
val adminScreens = listOf(
    AdminScreen.Dashboard,
    AdminScreen.Users,
    AdminScreen.Ads,
    AdminScreen.Reviews,
    AdminScreen.Statistics
)

// ─── Bottom Bar ───────────────────────────────────────────────────────────────

@Composable
fun AdminBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    Surface(
        tonalElevation  = 8.dp,
        shadowElevation = 12.dp,
        shape           = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color           = Color.White
    ) {
        NavigationBar(containerColor = Color.Transparent) {
            adminScreens.forEach { screen ->
                val selected  = currentRoute.startsWith(screen.route)
                val iconSize  by animateDpAsState(if (selected) 26.dp else 22.dp, label = "sz")
                val iconColor by animateColorAsState(if (selected) GreenPrimary else Color.Gray, label = "cl")

                NavigationBarItem(
                    selected        = selected,
                    onClick         = { if (!selected) onNavigate(screen.route) },
                    alwaysShowLabel = false,
                    colors          = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(screen.icon, screen.label, modifier = Modifier.size(iconSize), tint = iconColor)
                            Spacer(Modifier.height(4.dp))
                            AnimatedVisibility(visible = selected) {
                                Box(
                                    Modifier.height(3.dp).width(18.dp)
                                        .clip(RoundedCornerShape(50)).background(GreenPrimary)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminBottomBarPreview() {
    Mazra3tyTheme {
        AdminBottomBar(currentRoute = AdminScreen.Dashboard.route, onNavigate = {})
    }
}