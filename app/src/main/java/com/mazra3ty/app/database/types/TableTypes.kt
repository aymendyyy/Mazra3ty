package com.mazra3ty.app.database.types

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val full_name: String,
    val phone: String? = null,
    val role: String,
    val date_of_birth: String? = null,
    val is_deleted: Boolean = false,
    val is_banned: Boolean = false,
    val banned_at: String? = null,
    val banned_reason: String? = null,

    val created_at: String? = null
)

@Serializable
data class Profile(
    val user_id: String,                        // PK (no separate id column)

    val location: String? = null,
    val skills: List<String>? = null,           // text[] → List<String>
    val bio: String? = null,                    // moved here from User
    val image_url: String? = null,              // new column

    val created_at: String? = null
)

@Serializable
data class Job(
    val id: String,

    val title: String,
    val description: String,

    val farmer_id: String,

    val location: String? = null,
    val salary: Double? = null,

    val status: String = "open",

    val created_at: String? = null
)

@Serializable
data class Application(
    val id: String,

    val job_id: String,
    val worker_id: String,

    val status: String = "pending",

    val created_at: String? = null
)

@Serializable
data class Message(
    val id: String,

    val sender_id: String,
    val receiver_id: String,

    val content: String,

    val created_at: String? = null
)

@Serializable
data class Review(
    val id: String,

    val reviewer_id: String,
    val reviewed_id: String,

    val rating: Int,
    val comment: String? = null,

    val created_at: String? = null
)

@Serializable
data class UserImage(
    val id: String,

    val user_id: String,
    val image_url: String,

    val created_at: String? = null
)

@Serializable
data class WorkerPost(
    val id: String,
    val worker_id: String,
    val title: String,
    val description: String,
    val skills: List<String> = emptyList(),
    val location: String? = null,
    val status: WorkerPostStatus,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateJob(
    val title: String,
    val description: String,
    val farmer_id: String,
    val location: String? = null,
    val salary: Double? = null
)

@Serializable
data class CreateWorkerPost(
    val worker_id: String,
    val title: String,
    val description: String,
    val skills: List<String> = emptyList(),
    val location: String? = null,
    val status: WorkerPostStatus? = null
)

enum class WorkerPostStatus {
    active,
    inactive
}

@Serializable
data class Sponsor(
    val id: String,
    val name: String,
    val tagline: String? = null,
    val image_url: String? = null,
    val redirect_url: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null
)
enum class NotificationType {
    APPLICATION,            // Farmer receives: a worker applied to their job
    APPLICATION_ACCEPTED,   // Worker receives: their application was accepted
    APPLICATION_REJECTED,   // Worker receives: their application was rejected
    RATING                  // Both: someone left a review/rating
}

// ─── Notification Model ───────────────────────────────────────────────────────

@Serializable
data class Notification(
    val id: String,
    val user_id: String,
    val title: String,
    val message: String,
    val type: String,               // stored as string, matches NotificationType name
    val is_read: Boolean = false,
    val created_at: String? = null
) {
    val notificationType: NotificationType
        get() = NotificationType.valueOf(type)
}

// ─── Insert payload ───────────────────────────────────────────────────────────

@Serializable
data class CreateNotification(
    val user_id: String,
    val title: String,
    val message: String,
    val type: String
)
@Serializable
data class UserWithProfile(
    // ── from users ──────────────────────────────────────────
    val id: String,
    val full_name: String,
    val phone: String? = null,
    val email: String? = null,
    val role: String,
    val date_of_birth: String? = null,
    val is_banned: Boolean = false,
    val is_deleted: Boolean = false,
    val banned_at: String? = null,
    val banned_reason: String? = null,
    val created_at: String? = null,

    // ── from profiles (nullable – left join) ─────────────────
    val location: String? = null,
    val skills: List<String>? = null,
    val image_url: String? = null,
    val bio: String? = null
)