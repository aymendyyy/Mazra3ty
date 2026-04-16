package com.mazra3ty.app.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
public fun AdminHome(
    userEmail: String,
    onLogout:  () -> Unit
) {
    var currentRoute by remember { mutableStateOf(AdminScreen.Dashboard.route) }

    AdminGuard(onUnauthorized = onLogout) {
        Scaffold(
            containerColor = Color(0xFFF5F5F5),
            topBar = {
                // currentRoute is passed so the title updates on every navigation
                AdminTopBar(
                    currentRoute = currentRoute,
                    onLogout     = onLogout
                )
            },
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
