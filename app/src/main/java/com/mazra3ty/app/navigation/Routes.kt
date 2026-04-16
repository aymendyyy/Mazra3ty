package com.mazra3ty.app.navigation
// navigation/Routes.kt
object WorkerRoutes {
    const val HOME     = "worker_home"
    const val JOBS     = "worker_jobs"
    const val POSTS    = "worker_posts"
    const val MESSAGES = "worker_messages"
    const val PROFILE  = "worker_profile"
}

object FarmerRoutes {
    const val HOME         = "farmer_home"
    const val MY_JOBS      = "farmer_my_jobs"
    const val WORKER_FEED  = "farmer_worker_feed"
    const val MESSAGES     = "farmer_messages"
    const val PROFILE      = "farmer_profile"
}

object AdminRoutes {
    const val DASHBOARD   = "admin_dashboard"
    const val USERS       = "admin_users"
    const val ADS         = "admin_ads"
    const val REVIEWS     = "admin_reviews"
    const val STATISTICS  = "admin_statistics"
    const val REPORTS     = "admin_reports"
}

// Optional: a sealed class for type‑safe navigation (if you prefer)
sealed class Screen(val route: String) {
    // Worker
    object WorkerHome : Screen(WorkerRoutes.HOME)
    object WorkerJobs : Screen(WorkerRoutes.JOBS)
    object WorkerPosts : Screen(WorkerRoutes.POSTS)
    object WorkerMessages : Screen(WorkerRoutes.MESSAGES)
    object WorkerProfile : Screen(WorkerRoutes.PROFILE)

    // Farmer
    object FarmerHome : Screen(FarmerRoutes.HOME)
    object FarmerMyJobs : Screen(FarmerRoutes.MY_JOBS)
    object FarmerWorkerFeed : Screen(FarmerRoutes.WORKER_FEED)
    object FarmerMessages : Screen(FarmerRoutes.MESSAGES)
    object FarmerProfile : Screen(FarmerRoutes.PROFILE)

    // Admin
    object AdminDashboard : Screen(AdminRoutes.DASHBOARD)
    object AdminUsers : Screen(AdminRoutes.USERS)
    object AdminAds : Screen(AdminRoutes.ADS)
    object AdminReviews : Screen(AdminRoutes.REVIEWS)
    object AdminStatistics : Screen(AdminRoutes.STATISTICS)
    object AdminReports : Screen(AdminRoutes.REPORTS)
}