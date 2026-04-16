package com.mazra3ty.app.database.types

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val full_name: String,
    val email: String? = null,
    val phone: String? = null,
    val role: String,

    val date_of_birth: String? = null,
    val bio: String? = null,
    val is_deleted: Boolean = false,

    val is_banned: Boolean = false,
    val banned_at: String? = null,
    val banned_reason: String? = null,

    val created_at: String? = null
)
@Serializable
data class Profile(
    val id: String,

    val user_id: String,

    val location: String? = null,
    val skills: String? = null,

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

    val title: String? = null,
    val description: String,

    val skills: String? = null,
    val location: String? = null,

    val availability: String? = null,

    val created_at: String? = null
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
    val title: String? = null,
    val description: String,
    val skills: String? = null,
    val location: String? = null,
    val availability: String? = null
)