package com.mazra3ty.app.ui.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mazra3ty.app.notifications.AdminNotificationWorker

@Composable
fun AdminHome(
    onLogout:  () -> Unit
) {
    var currentRoute by remember { mutableStateOf(AdminScreen.Dashboard.route) }
    val context = LocalContext.current

    // ── Request POST_NOTIFICATIONS (Android 13+) & schedule background worker ─
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) AdminNotificationWorker.schedule(context)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) AdminNotificationWorker.schedule(context)
            else permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AdminNotificationWorker.schedule(context)
        }
    }

    AdminGuard(onUnauthorized = onLogout) {
        Scaffold(
            containerColor = Color(0xFFF5F5F5),
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
                    AdminScreen.Applications.route ->
                        ApplicationsMonitorScreen(
                            onBack = { currentRoute = AdminScreen.Dashboard.route }
                        )
                }
            }
        }
    }
}