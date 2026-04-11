package com.mazra3ty.app.ui.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.GreenPrimaryDark
import com.mazra3ty.app.ui.theme.Mazra3tyTheme

// ─── Model ───────────────────────────────────────────────────────────────────

sealed class AdminScreen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Dashboard : AdminScreen(
        route = "admin_dashboard",
        label = "Dashboard",
        icon = Icons.Outlined.Dashboard
    )
    object Users : AdminScreen(
        route = "admin_users",
        label = "Users",
        icon = Icons.Outlined.People
    )
    object Ads : AdminScreen(
        route = "admin_ads",
        label = "Ads",
        icon = Icons.Outlined.Work
    )
    object Reviews : AdminScreen(
        route = "admin_reviews",
        label = "Reviews",
        icon = Icons.Outlined.RateReview
    )
    object Reports : AdminScreen(
        route = "admin_reports",
        label = "Reports",
        icon = Icons.Outlined.Description
    )
    object Statistics : AdminScreen(
        route = "admin_statistics",
        label = "Stats",
        icon = Icons.Outlined.BarChart
    )
}

val adminScreens = listOf(
    AdminScreen.Dashboard,
    AdminScreen.Users,
    AdminScreen.Ads,
    AdminScreen.Reviews,
    AdminScreen.Reports,
    AdminScreen.Statistics
)

// ─── Bottom Bar Composable ────────────────────────────────────────────────────

@Composable
fun AdminBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        adminScreens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen.route) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label
                    )
                },
                label = {
                    Text(
                        text = screen.label,
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GreenPrimary,
                    selectedTextColor = GreenPrimary,
                    indicatorColor = GreenPrimary.copy(alpha = 0.12f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminBottomBarPreview() {
    Mazra3tyTheme {
        AdminBottomBar(
            currentRoute = AdminScreen.Dashboard.route,
            onNavigate = {}
        )
    }
}
