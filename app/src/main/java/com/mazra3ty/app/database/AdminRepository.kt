package com.mazra3ty.app.database

import com.mazra3ty.app.database.types.Application
import com.mazra3ty.app.database.types.Job
import com.mazra3ty.app.database.types.Profile
import com.mazra3ty.app.database.types.Review
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.database.types.UserWithProfile
import com.mazra3ty.app.database.types.WorkerPost
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant

class AdminRepository {

    private val db get() = SupabaseClientProvider.client.postgrest

    // ─── Users ────────────────────────────────────────────────────────────────

    /** Get all users with their profiles */
    suspend fun getAllUsersWithProfiles(): List<UserWithProfile> =
        db["users"].select(Columns.raw("*, profiles(*)")).decodeList()

    /** Get all users (compact) */
    suspend fun getAllUsers(): List<User> =
        db["users"].select().decodeList()

    /** Delete a user by id */
    suspend fun deleteUser(userId: String) {
        db["users"].delete {
            filter { eq("id", userId) }
        }
    }

    /** Ban a user */
    suspend fun banUser(userId: String, reason: String = "Banned by admin") {
        db["users"].update(
            mapOf(
                "is_banned"     to true,
                "banned_at"     to Instant.now().toString(),
                "banned_reason" to reason
            )
        ) {
            filter { eq("id", userId) }
        }
    }

    /** Unban a user */
    suspend fun unbanUser(userId: String) {
        db["users"].update(
            mapOf(
                "is_banned"     to false,
                "banned_at"     to null,
                "banned_reason" to null
            )
        ) {
            filter { eq("id", userId) }
        }
    }

    /** Get user by id with profile */
    suspend fun getUserById(userId: String): UserWithProfile =
        db["users"].select(Columns.raw("*, profiles(*)")) {
            filter { eq("id", userId) }
        }.decodeSingle()

    // ─── Jobs ─────────────────────────────────────────────────────────────────

    /** Get all jobs */
    suspend fun getAllJobs(): List<Job> =
        db["jobs"].select().decodeList()

    /** Delete a job by id */
    suspend fun deleteJob(jobId: String) {
        db["jobs"].delete {
            filter { eq("id", jobId) }
        }
    }

    /** Close a job (set status = closed) */
    suspend fun closeJob(jobId: String) {
        db["jobs"].update(mapOf("status" to "closed")) {
            filter { eq("id", jobId) }
        }
    }

    // ─── Reviews ──────────────────────────────────────────────────────────────

    /** Get all reviews */
    suspend fun getAllReviews(): List<Review> =
        db["reviews"].select().decodeList()

    /** Delete a review by id */
    suspend fun deleteReview(reviewId: String) {
        db["reviews"].delete {
            filter { eq("id", reviewId) }
        }
    }

    // ─── Applications ─────────────────────────────────────────────────────────

    /** Get all applications */
    suspend fun getAllApplications(): List<Application> =
        db["applications"].select().decodeList()

    // ─── Worker Posts ─────────────────────────────────────────────────────────

    /** Get all worker posts */
    suspend fun getAllWorkerPosts(): List<WorkerPost> =
        db["worker_posts"].select().decodeList()

    /** Delete a worker post */
    suspend fun deleteWorkerPost(postId: String) {
        db["worker_posts"].delete {
            filter { eq("id", postId) }
        }
    }

    // ─── Statistics ───────────────────────────────────────────────────────────

    data class AdminStats(
        val totalUsers: Int,
        val bannedUsers: Int,
        val workers: Int,
        val farmers: Int,
        val totalJobs: Int,
        val openJobs: Int,
        val closedJobs: Int,
        val totalReviews: Int,
        val avgRating: Float,
        val totalApplications: Int,
        val pendingApplications: Int,
        val acceptedApplications: Int
    )

    suspend fun getStats(): AdminStats {
        val users        = getAllUsers()
        val jobs         = getAllJobs()
        val reviews      = getAllReviews()
        val applications = getAllApplications()

        return AdminStats(
            totalUsers           = users.size,
            bannedUsers          = users.count { it.is_banned },
            workers              = users.count { it.role == "worker" },
            farmers              = users.count { it.role == "farmer" },
            totalJobs            = jobs.size,
            openJobs             = jobs.count { it.status == "open" },
            closedJobs           = jobs.count { it.status == "closed" },
            totalReviews         = reviews.size,
            avgRating            = if (reviews.isEmpty()) 0f
            else reviews.map { it.rating }.average().toFloat(),
            totalApplications    = applications.size,
            pendingApplications  = applications.count { it.status == "pending" },
            acceptedApplications = applications.count { it.status == "accepted" }
        )
    }
}
