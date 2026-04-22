package com.mazra3ty.app.ui.admin
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
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.mazra3ty.app.MainActivity
import com.mazra3ty.app.R
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
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

    override suspend fun doWork(): Result = coroutineScope {
        runCatching {

            val prefs = context.getSharedPreferences(PREFS_STAMP, Context.MODE_PRIVATE)
            val lastCheck = prefs.getString(KEY_LAST_CHECK, null)
                ?: Instant.now().minus(1, ChronoUnit.HOURS).toString()

            val newUsersDeferred = async {
                SupabaseClientProvider.client.postgrest["users"]
                    .select { filter { gte("created_at", lastCheck) } }
                    .decodeList<User>()
            }

            val newJobsDeferred = async {
                SupabaseClientProvider.client.postgrest["jobs"]
                    .select { filter { gte("created_at", lastCheck) } }
                    .decodeList<Job>()
            }

            val newReviewsDeferred = async {
                SupabaseClientProvider.client.postgrest["reviews"]
                    .select { filter { gte("created_at", lastCheck) } }
                    .decodeList<Review>()
            }

            val newAppsDeferred = async {
                SupabaseClientProvider.client.postgrest["applications"]
                    .select { filter { gte("created_at", lastCheck) } }
                    .decodeList<Application>()
            }

            val newUsers = newUsersDeferred.await()
            val newJobs = newJobsDeferred.await()
            val newReviews = newReviewsDeferred.await()
            val newApps = newAppsDeferred.await()

            Result.success()

        }.getOrElse {
            Result.retry()
        }
    }
    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "Mazra3ty Admin Alerts", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Admin activity alerts" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun pushNotification(id: Int, title: String, body: String, section: String) {
        // ← FIXED: Check Android 13+ notification permission
        if (!canPostNotifications()) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted - skipping notification")
            return
        }

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
        Log.d(TAG, "Posted system notification #$id: $title")
    }

    companion object {
        private const val TAG = "AdminNotifWorker"
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
            Log.d(TAG, "Worker scheduled")
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