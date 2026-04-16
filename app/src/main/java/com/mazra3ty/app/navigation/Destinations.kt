package com.mazra3ty.app.navigation

// navigation/Destinations.kt

import androidx.navigation.NavType
import androidx.navigation.navArgument

object Destinations {
    // Profile screen with userId argument
    const val PROFILE_WITH_ID = "profile/{userId}"

    fun profileRoute(userId: String) = "profile/$userId"

    val profileArgs = listOf(
        navArgument("userId") { type = NavType.StringType }
    )

    // Example for a job detail screen
    // const val JOB_DETAIL = "job/{jobId}"
    // fun jobDetailRoute(jobId: String) = "job/$jobId"
}