// navigation/AppNavHost.kt
package com.mazra3ty.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mazra3ty.app.ui.admin.AdminBottomBar
import com.mazra3ty.app.ui.admin.AdminHome
import com.mazra3ty.app.ui.admin.AdminTopBar
import com.mazra3ty.app.ui.admin.AdsMonitorScreen
import com.mazra3ty.app.ui.admin.ReportsScreen
import com.mazra3ty.app.ui.admin.ReviewsMonitorScreen
import com.mazra3ty.app.ui.admin.StatisticsScreen
import com.mazra3ty.app.ui.admin.UsersManagementScreen
import com.mazra3ty.app.ui.component.FarmerBottomBar
import com.mazra3ty.app.ui.farmer.FarmerHomeScreen
import com.mazra3ty.app.ui.farmer.FarmerJobsScreen
import com.mazra3ty.app.ui.farmer.JobDetailScreen
import com.mazra3ty.app.ui.farmer.NotificationsScreen
import com.mazra3ty.app.ui.farmer.WorkersPage
import com.mazra3ty.app.ui.screens.*

/**
 * Top‑level navigation host that decides which bottom bar + NavHost to show
 * based on the user's role.
 */

@Composable
fun AppNavHost(
    userId: String,
    userEmail: String,
    userRole: String,
    onLogout: () -> Unit
) {
    when (userRole) {
        "admin"  -> AdminNavHost(userId, userEmail, onLogout)
        "farmer" -> FarmerNavHost(userId, userEmail, onLogout)
        //else     -> WorkerNavHost(userId, userEmail, onLogout)   // worker
    }
}

// ─────────────────────────────────────────────────────────────
// Worker Navigation Host
// ─────────────────────────────────────────────────────────────

/*@Composable
private fun WorkerNavHost(
    userId: String,
    userEmail: String,
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
            userEmail = userEmail,
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}*/
/*

@Composable
private fun WorkerNavGraph(
    navController: NavHostController,
    userId: String,
    userEmail: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = WorkerRoutes.HOME,
        modifier = modifier
    ) {
        composable(WorkerRoutes.HOME) {
            WorkerDashboardScreen(userEmail = userEmail, onLogout = onLogout)
        }
        composable(WorkerRoutes.JOBS) {
            WorkerJobsScreen(userEmail = userEmail, onBack = { navController.popBackStack() })
        }
        composable(WorkerRoutes.POSTS) {
            WorkerMyPostsScreen(userEmail = userEmail, onBack = { navController.popBackStack() })
        }
        composable(WorkerRoutes.MESSAGES) {
            WorkerMessagesScreen(userEmail = userEmail, onBack = { navController.popBackStack() })
        }
        composable(WorkerRoutes.PROFILE) {
            ProfileScreen(userId = userId, onBack = { navController.popBackStack() })
        }
    }
}
*/

// ─────────────────────────────────────────────────────────────
// Farmer Navigation Host
// ─────────────────────────────────────────────────────────────

@Composable
private fun FarmerNavHost(
    userId: String,
    userEmail: String,
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
            userEmail = userEmail,
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun FarmerNavGraph(
    navController: NavHostController,
    userId: String,
    userEmail: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = FarmerRoutes.HOME,
        modifier = modifier
    ) {
        // Home screen
        composable(FarmerRoutes.HOME) {
            FarmerHomeScreen(
                userId = userId,
                onWorkerProfile = { workerId ->
                    navController.navigate("profile/$workerId")
                },
                onJobDetail = { jobId ->
                    navController.navigate("job_detail/$jobId")
                },
                onViewAllWorkers = {
                    navController.navigate(FarmerRoutes.WORKER_FEED)
                },
                onViewAllJobs = {
                    navController.navigate(FarmerRoutes.MY_JOBS)
                },
                onCreateJob = {
                    navController.navigate("create_job")
                },
                onViewNotifications = {
                    navController.navigate("notifications")
                }
            )
        }

        composable("job_detail/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            JobDetailScreen(jobId = jobId, currentUserId = userId, onBack = { navController.popBackStack() })
        }
        // My Jobs screen
        composable(FarmerRoutes.MY_JOBS) {
            FarmerJobsScreen(
                farmerId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        // Worker Feed screen (WorkersPage)
        composable(FarmerRoutes.WORKER_FEED) {
            WorkersPage(
                currentFarmerId = userId,
                onBack = { navController.popBackStack() },
                onSendOffer = { workerPost, worker ->
                    // Handle sending offer (e.g., navigate to a chat or create a message)
                },
                //showBackButton = false
            )
        }

        // Messages screen
        /*composable(FarmerRoutes.MESSAGES) {
            FarmerMessagesScreen(
                userEmail = userEmail,
                onBack = { navController.popBackStack() }
            )
        }*/

        // Profile screen
        composable(FarmerRoutes.PROFILE) {
            ProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }

        // Optional additional destinations (job detail, create job, notifications)
        composable("profile/{workerId}") { backStackEntry ->
            val workerId = backStackEntry.arguments?.getString("workerId") ?: return@composable
            ProfileScreen(
                userId = workerId,
                onBack = { navController.popBackStack() }
            )
        }

   /*     composable("create_job") {
            CreateJobScreen(
                *//*farmerId = userId,
                onJobCreated = { navController.popBackStack() },
                onBack = { navController.popBackStack() }*//*
            )
        }*/

        composable("notifications") {
            NotificationsScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
// ─────────────────────────────────────────────────────────────
// Admin Navigation Host (example – adapt to your Admin screens)
// ─────────────────────────────────────────────────────────────

@Composable
private fun AdminNavHost(
    userId: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(AdminRoutes.DASHBOARD) }

    Scaffold(
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
                onNavigate = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        popUpTo(AdminRoutes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AdminRoutes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AdminRoutes.DASHBOARD) {
                AdminHome(
                    onNavigate = { /* navigate inside admin */ },
                    userEmail ="" ,
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