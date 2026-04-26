package com.mazra3ty.app.ui.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.mazra3ty.app.MainActivity
import com.mazra3ty.app.R
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.*
import com.mazra3ty.app.notifications.NotificationStore
import com.mazra3ty.app.notifications.PersistedNotification
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit

// ─── WorkManager worker ───────────────────────────────────────────────────────

class AdminNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_STAMP, Context.MODE_PRIVATE)
            val lastCheck = prefs.getString(KEY_LAST_CHECK, null)
                ?: Instant.now().minus(1, ChronoUnit.HOURS).toString()

            Log.d(TAG, "Worker checking activity since: $lastCheck")
            createNotificationChannel()

            // ── Fetch all new activity in parallel ────────────────────────
            val newUsersDeferred = async {
                runCatching {
                    SupabaseClientProvider.client.postgrest["users"]
                        .select { filter { gte("created_at", lastCheck) } }
                        .decodeList<User>()
                }.getOrElse { emptyList() }
            }

            val newJobsDeferred = async {
                runCatching {
                    SupabaseClientProvider.client.postgrest["jobs"]
                        .select { filter { gte("created_at", lastCheck) } }
                        .decodeList<Job>()
                }.getOrElse { emptyList() }
            }

            val newReviewsDeferred = async {
                runCatching {
                    SupabaseClientProvider.client.postgrest["reviews"]
                        .select { filter { gte("created_at", lastCheck) } }
                        .decodeList<Review>()
                }.getOrElse { emptyList() }
            }

            val newAppsDeferred = async {
                runCatching {
                    SupabaseClientProvider.client.postgrest["applications"]
                        .select { filter { gte("created_at", lastCheck) } }
                        .decodeList<Application>()
                }.getOrElse { emptyList() }
            }

            val newWorkerPostsDeferred = async {
                runCatching {
                    SupabaseClientProvider.client.postgrest["worker_posts"]
                        .select { filter { gte("created_at", lastCheck) } }
                        .decodeList<WorkerPost>()
                }.getOrElse { emptyList() }
            }

            val newDbNotificationsDeferred = async {
                runCatching {
                    // جلب جميع الإشعارات من جدول notifications بدون تصفية user_id
                    SupabaseClientProvider.client.postgrest["notifications"]
                        .select { filter { gte("created_at", lastCheck) } }
                        .decodeList<Notification>()
                }.getOrElse { emptyList() }
            }

            val newUsers       = newUsersDeferred.await()
            val newJobs        = newJobsDeferred.await()
            val newReviews     = newReviewsDeferred.await()
            val newApps        = newAppsDeferred.await()
            val newWorkerPosts = newWorkerPostsDeferred.await()
            val newDbNotifs    = newDbNotificationsDeferred.await()

            val incoming = mutableListOf<PersistedNotification>()
            var sysNotifId = NOTIF_BASE_ID

            // ── Process Notifications Table ───────────────────────────────
            newDbNotifs.forEach { dbNotif ->
                val notif = PersistedNotification(
                    id        = "db_notif_${dbNotif.id}",
                    iconKey   = when(dbNotif.type) {
                        "APPLICATION" -> "assignment"
                        "RATING"      -> "star"
                        else          -> "notifications"
                    },
                    iconColor = 0xFF9C27B0L, // لون أرجواني لتمييز إشعارات النظام
                    title     = dbNotif.title,
                    subtitle  = dbNotif.message,
                    route     = when(dbNotif.type) {
                        "APPLICATION" -> "admin_ads" // توجيه لقسم الطلبات
                        "RATING"      -> "admin_reviews"
                        else          -> "admin_dashboard"
                    },
                    timeLabel = timeAgo(dbNotif.created_at),
                    timestamp = dbNotif.created_at ?: Instant.now().toString()
                )
                incoming.add(notif)
                
                if (canPostNotifications()) {
                    postSystemNotif(
                        id      = sysNotifId++,
                        title   = dbNotif.title,
                        body    = dbNotif.message,
                        section = notif.route
                    )
                }
            }

            // ── Build PersistedNotification for each new item ─────────────

            // New user registrations
            newUsers.forEach { user ->
                val roleLabel = user.role.replaceFirstChar { it.uppercase() }
                val notif = PersistedNotification(
                    id        = "user_${user.id}",
                    iconKey   = "person_add",
                    iconColor = 0xFF4CAF50L,
                    title     = "New $roleLabel Registered",
                    subtitle  = "${user.full_name} · ${user.phone ?: user.email ?: "No contact"}",
                    route     = AdminScreen.Users.route,
                    timeLabel = timeAgo(user.created_at),
                    timestamp = user.created_at ?: Instant.now().toString()
                )
                incoming.add(notif)
                if (canPostNotifications()) {
                    postSystemNotif(
                        id      = sysNotifId++,
                        title   = "New $roleLabel Registered 👤",
                        body    = "${user.full_name} joined Mazra3ty",
                        section = AdminScreen.Users.route
                    )
                }
            }

            // New job posts
            newJobs.forEach { job ->
                val notif = PersistedNotification(
                    id        = "job_${job.id}",
                    iconKey   = "work",
                    iconColor = 0xFF1E88E5L,
                    title     = "New Job Posted",
                    subtitle  = "${job.title}${job.location?.let { " · $it" } ?: ""}${job.salary?.let { " · ${it.toInt()} DZD/J" } ?: ""}",
                    route     = AdminScreen.Ads.route,
                    timeLabel = timeAgo(job.created_at),
                    timestamp = job.created_at ?: Instant.now().toString()
                )
                incoming.add(notif)
                if (canPostNotifications()) {
                    postSystemNotif(
                        id      = sysNotifId++,
                        title   = "New Job Post 💼",
                        body    = "${job.title}${job.location?.let { " in $it" } ?: ""}",
                        section = AdminScreen.Ads.route
                    )
                }
            }

            // New applications
            newApps.forEach { app ->
                val notif = PersistedNotification(
                    id        = "app_${app.id}",
                    iconKey   = "assignment",
                    iconColor = 0xFFE91E63L,
                    title     = "New Application Submitted",
                    subtitle  = "Status: ${app.status.replaceFirstChar { it.uppercase() }} · ${timeAgo(app.created_at)}",
                    route     = AdminScreen.Applications.route,
                    timeLabel = timeAgo(app.created_at),
                    timestamp = app.created_at ?: Instant.now().toString()
                )
                incoming.add(notif)
                if (canPostNotifications()) {
                    postSystemNotif(
                        id      = sysNotifId++,
                        title   = "New Job Application 📋",
                        body    = "A worker applied for a job — status: ${app.status}",
                        section = AdminScreen.Applications.route
                    )
                }
            }

            // New reviews
            newReviews.forEach { review ->
                val stars = "★".repeat(review.rating) + "☆".repeat(5 - review.rating)
                val notif = PersistedNotification(
                    id        = "review_${review.id}",
                    iconKey   = "star",
                    iconColor = 0xFFFFC107L,
                    title     = "New Review Submitted",
                    subtitle  = "$stars${review.comment?.let { " · ${it.take(50)}" } ?: ""}",
                    route     = AdminScreen.Reviews.route,
                    timeLabel = timeAgo(review.created_at),
                    timestamp = review.created_at ?: Instant.now().toString()
                )
                incoming.add(notif)
                if (canPostNotifications()) {
                    postSystemNotif(
                        id      = sysNotifId++,
                        title   = "New Review $stars",
                        body    = review.comment?.take(80) ?: "${review.rating}-star review received",
                        section = AdminScreen.Reviews.route
                    )
                }
            }

            // New worker posts
            newWorkerPosts.forEach { post ->
                val notif = PersistedNotification(
                    id        = "wpost_${post.id}",
                    iconKey   = "work",
                    iconColor = 0xFF388E3CL,
                    title     = "New Worker Post",
                    subtitle  = "${post.title}${post.location?.let { " · $it" } ?: ""}",
                    route     = AdminScreen.Ads.route,
                    timeLabel = timeAgo(post.created_at),
                    timestamp = post.created_at ?: Instant.now().toString()
                )
                incoming.add(notif)
                if (canPostNotifications()) {
                    postSystemNotif(
                        id      = sysNotifId++,
                        title   = "New Worker Post 🌾",
                        body    = "${post.title}${post.location?.let { " · $it" } ?: ""}",
                        section = AdminScreen.Ads.route
                    )
                }
            }

            // ── Save to persistent store & update lastCheck timestamp ──────
            if (incoming.isNotEmpty()) {
                NotificationStore.addAll(context, incoming)
                Log.d(TAG, "Saved ${incoming.size} new notifications")
            }

            // Always update the last-check timestamp so we don't re-process items
            prefs.edit()
                .putString(KEY_LAST_CHECK, Instant.now().toString())
                .apply()

            Result.success()

        }.getOrElse { e ->
            Log.e(TAG, "Worker failed: ${e.message}", e)
            Result.retry()
        }
    }

    // ─── Channel ──────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mazra3ty Admin Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Real-time admin activity alerts"
            enableLights(true)
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    // ─── Permission check ─────────────────────────────────────────────────────

    private fun canPostNotifications(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

    // ─── Post a system push notification ─────────────────────────────────────

    private fun postSystemNotif(id: Int, title: String, body: String, section: String) {
        val pendingIntent = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_SECTION, section)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
        Log.d(TAG, "Posted system notification #$id: $title")
    }

    // ─── Companion ────────────────────────────────────────────────────────────

    companion object {
        private const val TAG            = "AdminNotifWorker"
        const val CHANNEL_ID             = "mazra3ty_admin_alerts"
        const val WORK_NAME              = "mazra3ty_admin_notification_worker"
        const val PREFS_STAMP            = "mazra3ty_notif_stamp"
        const val KEY_LAST_CHECK         = "last_check_timestamp"
        const val EXTRA_SECTION          = "open_section"
        private const val NOTIF_BASE_ID  = 2000

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AdminNotificationWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()
            )
            Log.d(TAG, "Periodic worker scheduled (every 15 min)")
        }

        fun cancel(context: Context) =
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun timeAgo(createdAt: String?): String = runCatching {
    val instant = Instant.parse(createdAt ?: return "—")
    val days    = ChronoUnit.DAYS.between(instant, Instant.now())
    val hours   = ChronoUnit.HOURS.between(instant, Instant.now())
    val minutes = ChronoUnit.MINUTES.between(instant, Instant.now())
    when {
        days    > 0 -> "${days}d ago"
        hours   > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else        -> "Just now"
    }
}.getOrElse { "—" }