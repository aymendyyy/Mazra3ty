package com.mazra3ty.app.notifications

/**
 * ─────────────────────────────────────────────────────────────────────────────
 *  REQUIRED dependencies (add to build.gradle.kts):
 *    implementation("androidx.work:work-runtime-ktx:2.9.1")
 *
 *  AndroidManifest.xml:
 *    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
 * ─────────────────────────────────────────────────────────────────────────────
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.mazra3ty.app.MainActivity
import com.mazra3ty.app.R
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

// ─── Persisted notification data class ───────────────────────────────────────

@Serializable
data class PersistedNotification(
    val id:        String,
    val iconKey:   String,   // "person_add" | "work" | "star" | "assignment"
    val iconColor: Long,     // ARGB packed as Long
    val title:     String,
    val subtitle:  String,
    val route:     String,
    val timeLabel: String,
    val timestamp: String    // ISO-8601, used for sorting
)

// ─── Notification store ───────────────────────────────────────────────────────
//
// Notifications are NEVER automatically deleted. The admin can:
//   - Delete a single notification  →  NotificationStore.deleteOne(context, id)
//   - Delete all notifications      →  NotificationStore.clearAll(context)
//
object NotificationStore {

    private const val PREFS_NAME = "mazra3ty_notifications"
    private const val KEY_LIST   = "notification_list"

    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<PersistedNotification> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LIST, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<PersistedNotification>>(raw) }
            .getOrElse { emptyList() }
    }

    fun save(context: Context, list: List<PersistedNotification>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIST, json.encodeToString(list))
            .apply()
    }

    /** Prepend new items, deduplicate by id, cap at 500 entries. */
    fun addAll(context: Context, incoming: List<PersistedNotification>) {
        val existing    = load(context)
        val existingIds = existing.map { it.id }.toSet()
        val merged      = incoming.filter { it.id !in existingIds } + existing
        save(context, merged.take(500))
    }

    fun deleteOne(context: Context, id: String) {
        save(context, load(context).filter { it.id != id })
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_LIST).apply()
    }
}

// ─── WorkManager worker ───────────────────────────────────────────────────────

class AdminNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val prefs     = context.getSharedPreferences(PREFS_STAMP, Context.MODE_PRIVATE)
            val lastCheck = prefs.getString(KEY_LAST_CHECK, null)
                ?: Instant.now().minus(1, ChronoUnit.HOURS).toString()

            createNotificationChannel()

            val incoming   = mutableListOf<PersistedNotification>()
            var sysNotifId = NOTIF_BASE_ID

            // ── New users ─────────────────────────────────────────────────
            val newUsers = SupabaseClientProvider.client.postgrest["users"]
                .select { filter { gte("created_at", lastCheck) } }
                .decodeList<User>()

            newUsers.forEach { user ->
                incoming += PersistedNotification(
                    id        = "user_${user.id}",
                    iconKey   = "person_add",
                    iconColor = 0xFF8BC34A,
                    title     = "New user registered",
                    subtitle  = "${user.full_name} · ${user.role.replaceFirstChar { it.uppercase() }}",
                    route     = "admin_users",
                    timeLabel = timeAgo(user.created_at),
                    timestamp = user.created_at ?: Instant.now().toString()
                )
            }
            if (newUsers.isNotEmpty()) pushNotification(
                id      = sysNotifId++,
                title   = "👤 ${newUsers.size} new user(s)",
                body    = newUsers.joinToString(", ") { it.full_name }.take(100),
                section = "admin_users"
            )

            // ── New jobs ──────────────────────────────────────────────────
            val newJobs = SupabaseClientProvider.client.postgrest["jobs"]
                .select { filter { gte("created_at", lastCheck) } }
                .decodeList<Job>()

            newJobs.forEach { job ->
                incoming += PersistedNotification(
                    id        = "job_${job.id}",
                    iconKey   = "work",
                    iconColor = 0xFF1E88E5,
                    title     = "New job posted",
                    subtitle  = job.title + (job.location?.let { " · $it" } ?: ""),
                    route     = "admin_ads",
                    timeLabel = timeAgo(job.created_at),
                    timestamp = job.created_at ?: Instant.now().toString()
                )
            }
            if (newJobs.isNotEmpty()) pushNotification(
                id      = sysNotifId++,
                title   = "💼 ${newJobs.size} new job(s)",
                body    = newJobs.joinToString(", ") { it.title }.take(100),
                section = "admin_ads"
            )

            // ── New reviews ───────────────────────────────────────────────
            val newReviews = SupabaseClientProvider.client.postgrest["reviews"]
                .select { filter { gte("created_at", lastCheck) } }
                .decodeList<Review>()

            newReviews.forEach { review ->
                incoming += PersistedNotification(
                    id        = "review_${review.id}",
                    iconKey   = "star",
                    iconColor = 0xFFFFC107,
                    title     = "New review — ${"★".repeat(review.rating)}",
                    subtitle  = review.comment?.take(60) ?: "No comment",
                    route     = "admin_reviews",
                    timeLabel = timeAgo(review.created_at),
                    timestamp = review.created_at ?: Instant.now().toString()
                )
            }
            if (newReviews.isNotEmpty()) pushNotification(
                id      = sysNotifId++,
                title   = "⭐ ${newReviews.size} new review(s)",
                body    = "Avg rating: ${"%.1f".format(newReviews.map { it.rating }.average())} ★",
                section = "admin_reviews"
            )

            // ── New applications ──────────────────────────────────────────
            val newApps = SupabaseClientProvider.client.postgrest["applications"]
                .select { filter { gte("created_at", lastCheck) } }
                .decodeList<Application>()

            newApps.forEach { app ->
                incoming += PersistedNotification(
                    id        = "app_${app.id}",
                    iconKey   = "assignment",
                    iconColor = 0xFFE91E63,
                    title     = "New application",
                    subtitle  = "Status: ${app.status.replaceFirstChar { it.uppercase() }}",
                    route     = "admin_applications",
                    timeLabel = timeAgo(app.created_at),
                    timestamp = app.created_at ?: Instant.now().toString()
                )
            }
            if (newApps.isNotEmpty()) pushNotification(
                id      = sysNotifId,
                title   = "📋 ${newApps.size} new application(s)",
                body    = "${newApps.count { it.status == "pending" }} pending",
                section = "admin_applications"
            )

            // ── Persist without erasing anything ──────────────────────────
            if (incoming.isNotEmpty()) NotificationStore.addAll(context, incoming)

            // Update last-check stamp
            prefs.edit().putString(KEY_LAST_CHECK, Instant.now().toString()).apply()

            Result.success()
        }.getOrElse { it.printStackTrace(); Result.retry() }
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "Mazra3ty Admin Alerts", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Admin activity alerts" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun pushNotification(id: Int, title: String, body: String, section: String) {
        val pi = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_SECTION, section)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, n)
    }

    companion object {
        const val CHANNEL_ID     = "mazra3ty_admin_alerts"
        const val WORK_NAME      = "mazra3ty_admin_notification_worker"
        const val PREFS_STAMP    = "mazra3ty_notif_stamp"
        const val KEY_LAST_CHECK = "last_check_timestamp"
        const val EXTRA_SECTION  = "open_section"
        private const val NOTIF_BASE_ID = 2000

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AdminNotificationWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()
            )
        }

        fun cancel(context: Context) =
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

private fun timeAgo(createdAt: String?): String = runCatching {
    val instant = Instant.parse(createdAt ?: return "—")
    val days    = ChronoUnit.DAYS.between(instant, Instant.now())
    val hours   = ChronoUnit.HOURS.between(instant, Instant.now())
    val minutes = ChronoUnit.MINUTES.between(instant, Instant.now())
    when {
        days    > 0  -> "${days}d ago"
        hours   > 0  -> "${hours}h ago"
        minutes > 0  -> "${minutes}m ago"
        else         -> "Just now"
    }
}.getOrElse { "—" }