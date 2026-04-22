// navigation/AppNavHost.kt
package com.mazra3ty.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

import com.mazra3ty.app.ui.admin.*
import com.mazra3ty.app.ui.component.*
import com.mazra3ty.app.ui.farmer.*
import com.mazra3ty.app.ui.worker.JobsScreen
import com.mazra3ty.app.ui.screens.*

@Composable
fun AppNavHost(
    userId: String,
    userRole: String,
    onLogout: () -> Unit
) {
    when (userRole) {
        "admin"  -> AdminNavHost(userId, onLogout)
        "farmer" -> FarmerNavHost(userId, onLogout)
        "worker" -> WorkerNavHost(userId, onLogout)
    }
}

// ─────────────────────────────────────────────
// Worker
// ─────────────────────────────────────────────

@Composable
private fun WorkerNavHost(
    userId: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(WorkerRoutes.HOME) }

    Scaffold(
        bottomBar = {
            WorkerBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        popUpTo(WorkerRoutes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        WorkerNavGraph(
            navController = navController,
            userId = userId,
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun WorkerNavGraph(
    navController: NavHostController,
    userId: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = WorkerRoutes.HOME,
        modifier = modifier
    ) {
        composable(WorkerRoutes.HOME) {
            JobsScreen()
        }
        composable(WorkerRoutes.PROFILE) {
            ProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ─────────────────────────────────────────────
// Farmer
// ─────────────────────────────────────────────

@Composable
private fun FarmerNavHost(
    userId: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(FarmerRoutes.HOME) }

    Scaffold(
        bottomBar = {
            FarmerBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        popUpTo(FarmerRoutes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        FarmerNavGraph(
            navController = navController,
            userId = userId,
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun FarmerNavGraph(
    navController: NavHostController,
    userId: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = FarmerRoutes.HOME,
        modifier = modifier
    ) {
        composable(FarmerRoutes.HOME) {
            FarmerHomeScreen(
                userId = userId,
                onWorkerProfile    = { workerId -> navController.navigate("profile/$workerId") },
                onJobDetail        = { jobId    -> navController.navigate("job_detail/$jobId") },
                onViewAllWorkers   = { navController.navigate(FarmerRoutes.WORKER_FEED) },
                onViewAllJobs      = { navController.navigate(FarmerRoutes.MY_JOBS) },
                onCreateJob        = { navController.navigate("create_job") },
                onViewNotifications= { navController.navigate("notifications") }
            )
        }

        composable("job_detail/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            JobDetailScreen(
                jobId = jobId,
                currentUserId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(FarmerRoutes.MY_JOBS) {
            FarmerJobsScreen(
                farmerId = userId,
                onBack   = { navController.popBackStack() }
            )
        }

        composable(FarmerRoutes.WORKER_FEED) {
            WorkersPage(
                currentFarmerId = userId,
                onBack          = { navController.popBackStack() },
                onSendOffer     = { _, _ -> }
            )
        }

        composable(FarmerRoutes.PROFILE) {
            ProfileScreen(
                userId   = userId,
                onBack   = { navController.popBackStack() },
                onLogout = onLogout
            )
        }

        composable("profile/{workerId}") { backStackEntry ->
            val workerId = backStackEntry.arguments?.getString("workerId") ?: return@composable
            ProfileScreen(
                userId = workerId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("notifications") {
            NotificationsScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ─────────────────────────────────────────────
// Admin
// ─────────────────────────────────────────────

@Composable
private fun AdminNavHost(
    userId: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(AdminRoutes.DASHBOARD) }

    Scaffold(
        topBar = {
            AdminTopBar(
                currentRoute = currentRoute,
                onLogout     = onLogout,
                onNavigate   = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        popUpTo(AdminRoutes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        bottomBar = {
            AdminBottomBar(
                currentRoute = currentRoute,
                onNavigate   = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        popUpTo(AdminRoutes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = AdminRoutes.DASHBOARD,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(AdminRoutes.DASHBOARD) {
                AdminDashboardScreen(
                    onNavigate = { route ->
                        currentRoute = route
                        navController.navigate(route) {
                            popUpTo(AdminRoutes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    onLogout = onLogout
                )
            }

            composable(AdminRoutes.USERS) {
                UsersManagementScreen(onBack = { navController.popBackStack() })
            }
            composable(AdminRoutes.ADS) {
                AdsMonitorScreen(onBack = { navController.popBackStack() })
            }
            composable(AdminRoutes.REVIEWS) {
                ReviewsMonitorScreen(onBack = { navController.popBackStack() })
            }
            composable(AdminRoutes.STATISTICS) {
                StatisticsScreen(onBack = { navController.popBackStack() })
            }
            composable(AdminRoutes.REPORTS) {
                ReportsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}