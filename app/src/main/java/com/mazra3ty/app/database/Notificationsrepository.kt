package com.mazra3ty.app.database
import com.mazra3ty.app.database.types.CreateNotification
import com.mazra3ty.app.database.types.Notification
import com.mazra3ty.app.database.types.NotificationType
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class NotificationsRepository {

    private val db get() = SupabaseClientProvider.client.postgrest

    // ─── Fetch ────────────────────────────────────────────────────────────────

    /** All notifications for a given user, newest first */
    suspend fun getNotificationsForUser(userId: String): List<Notification> =
        db["notifications"]
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()

    /** Only unread notifications */
    suspend fun getUnreadNotifications(userId: String): List<Notification> =
        db["notifications"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()

    /** Count of unread notifications (for badge) */
    suspend fun getUnreadCount(userId: String): Int =
        getUnreadNotifications(userId).size

    // ─── Mark as read ─────────────────────────────────────────────────────────

    /** Mark a single notification as read */
    suspend fun markAsRead(notificationId: String) {
        db["notifications"]
            .update(mapOf("is_read" to true)) {
                filter { eq("id", notificationId) }
            }
    }

    /** Mark ALL notifications for a user as read */
    suspend fun markAllAsRead(userId: String) {
        db["notifications"]
            .update(mapOf("is_read" to true)) {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
            }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    suspend fun deleteNotification(notificationId: String) {
        db["notifications"]
            .delete { filter { eq("id", notificationId) } }
    }

    // ─── Create helpers ───────────────────────────────────────────────────────

    /**
     * Creates a notification.
     * Called programmatically whenever a trigger condition is met.
     */
    suspend fun createNotification(notification: CreateNotification) {
        db["notifications"].insert(notification)
    }

    // ─── Domain-level helpers ─────────────────────────────────────────────────

    /**
     * Called when a worker applies to a job.
     * Notifies the FARMER who owns the job.
     *
     * @param farmerId    the job owner
     * @param workerName  the worker's display name
     * @param jobTitle    the job title
     */
    suspend fun notifyFarmerNewApplication(
        farmerId: String,
        workerName: String,
        jobTitle: String
    ) {
        createNotification(
            CreateNotification(
                user_id = farmerId,
                title   = "New Application Received",
                message = "$workerName applied to your job: \"$jobTitle\"",
                type    = NotificationType.APPLICATION.name
            )
        )
    }

    /**
     * Called when a farmer accepts a worker's application.
     * Notifies the WORKER.
     *
     * @param workerId  the applicant
     * @param jobTitle  the accepted job
     */
    suspend fun notifyWorkerApplicationAccepted(
        workerId: String,
        jobTitle: String
    ) {
        createNotification(
            CreateNotification(
                user_id = workerId,
                title   = "Application Accepted 🎉",
                message = "Congratulations! Your application for \"$jobTitle\" was accepted.",
                type    = NotificationType.APPLICATION_ACCEPTED.name
            )
        )
    }

    /**
     * Called when a farmer rejects a worker's application.
     * Notifies the WORKER.
     */
    suspend fun notifyWorkerApplicationRejected(
        workerId: String,
        jobTitle: String
    ) {
        createNotification(
            CreateNotification(
                user_id = workerId,
                title   = "Application Update",
                message = "Unfortunately your application for \"$jobTitle\" was not selected.",
                type    = NotificationType.APPLICATION_REJECTED.name
            )
        )
    }

    /**
     * Called when someone leaves a review/rating.
     * Notifies the REVIEWED user.
     *
     * @param reviewedUserId  the person who received the review
     * @param reviewerName    the reviewer's display name
     * @param rating          the star rating (1-5)
     */
    suspend fun notifyUserNewRating(
        reviewedUserId: String,
        reviewerName: String,
        rating: Int
    ) {
        val stars = "⭐".repeat(rating)
        createNotification(
            CreateNotification(
                user_id = reviewedUserId,
                title   = "New Review Received",
                message = "$reviewerName left you a $stars review.",
                type    = NotificationType.RATING.name
            )
        )
    }
}
