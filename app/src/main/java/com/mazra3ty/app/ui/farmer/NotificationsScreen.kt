package com.mazra3ty.app.ui.farmer

import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.NotificationsRepository
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Notification
import com.mazra3ty.app.database.types.NotificationType
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─── Filter tabs ─────────────────────────────────────────────────────────────

private enum class NotificationFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread")
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    userId: String,
    onBack: () -> Unit = {}
) {
    val repo = remember { NotificationsRepository() }
    val scope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var activeFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    var showMarkAllDialog by remember { mutableStateOf(false) }

    // ── Load ──────────────────────────────────────────────────────────────────
    fun loadNotifications() {
        scope.launch {
            try {
                notifications = repo.getNotificationsForUser(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadNotifications() }

    // ── Realtime subscription ─────────────────────────────────────────────────
    LaunchedEffect(userId) {
        try {
            val channel = SupabaseClientProvider.client.realtime.channel("notifications:$userId")
            channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(
                schema = "public"
            ) {
                table = "notifications"
            }.onEach { _ ->
                // Re-fetch when a new notification row lands for this user
                notifications = repo.getNotificationsForUser(userId)
            }.launchIn(this)
            channel.subscribe()
        } catch (_: Exception) {
            // Realtime is optional – gracefully ignore if not configured
        }
    }

    // ── Filtered list ─────────────────────────────────────────────────────────
    val displayedNotifications = remember(notifications, activeFilter) {
        when (activeFilter) {
            NotificationFilter.ALL    -> notifications
            NotificationFilter.UNREAD -> notifications.filter { !it.is_read }
        }
    }

    val unreadCount = remember(notifications) { notifications.count { !it.is_read } }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(GreenPrimary)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { showMarkAllDialog = true }) {
                            Text(
                                "Mark all read",
                                color = GreenPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Filter chips ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NotificationFilter.entries.forEach { filter ->
                    val isSelected = activeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeFilter = filter },
                        label = {
                            Text(
                                text = if (filter == NotificationFilter.UNREAD && unreadCount > 0)
                                    "${filter.label} ($unreadCount)"
                                else
                                    filter.label,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = GreenPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = GreenPrimary,
                            borderColor = Color(0xFFDDDDDD)
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // ── Content ───────────────────────────────────────────────────────
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenPrimary)
                }

                displayedNotifications.isEmpty() -> EmptyNotificationsState(
                    filter = activeFilter
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = displayedNotifications,
                        key = { it.id }
                    ) { notification ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + slideInVertically { it / 2 }
                        ) {
                            SwipeableNotificationCard(
                                notification = notification,
                                onMarkRead = {
                                    scope.launch {
                                        try {
                                            repo.markAsRead(notification.id)
                                            notifications = notifications.map {
                                                if (it.id == notification.id) it.copy(is_read = true) else it
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            repo.deleteNotification(notification.id)
                                            notifications = notifications.filter { it.id != notification.id }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    // ── Mark all read dialog ──────────────────────────────────────────────────
    if (showMarkAllDialog) {
        AlertDialog(
            onDismissRequest = { showMarkAllDialog = false },
            title = { Text("Mark All as Read") },
            text = { Text("All $unreadCount unread notifications will be marked as read.") },
            confirmButton = {
                TextButton(onClick = {
                    showMarkAllDialog = false
                    scope.launch {
                        try {
                            repo.markAllAsRead(userId)
                            notifications = notifications.map { it.copy(is_read = true) }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }) {
                    Text("Confirm", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Swipeable card wrapper ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationCard(
    notification: Notification,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!notification.is_read) onMarkRead()
                    false  // don't dismiss – just mark read
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isDeleteSwipe = direction == SwipeToDismissBoxValue.EndToStart
            val isReadSwipe   = direction == SwipeToDismissBoxValue.StartToEnd

            val bgColor by animateColorAsState(
                targetValue = when {
                    isDeleteSwipe -> RedError.copy(alpha = 0.85f)
                    isReadSwipe   -> GreenPrimary.copy(alpha = 0.85f)
                    else          -> Color.Transparent
                },
                label = "swipe_bg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isDeleteSwipe) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (isDeleteSwipe) {
                    Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(24.dp))
                } else if (isReadSwipe && !notification.is_read) {
                    Icon(Icons.Outlined.DoneAll, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    ) {
        NotificationCard(
            notification = notification,
            onMarkRead = onMarkRead
        )
    }
}

// ─── Notification Card ────────────────────────────────────────────────────────

@Composable
private fun NotificationCard(
    notification: Notification,
    onMarkRead: () -> Unit
) {
    val type = remember(notification.type) {
        runCatching { NotificationType.valueOf(notification.type) }
            .getOrDefault(NotificationType.APPLICATION)
    }

    val (iconVector, iconBg, iconTint) = remember(type) {
        notificationStyle(type)
    }

    val timeLabel = remember(notification.created_at) {
        formatTimeAgo(notification.created_at)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.is_read) Color.White
            else Color(0xFFEEF7E8)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (notification.is_read) Color(0xFFEEEEEE)
            else GreenPrimary.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !notification.is_read) { onMarkRead() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.is_read) FontWeight.Medium else FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (!notification.is_read) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary)
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = if (notification.is_read) GrayDark else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Type chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(iconBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel(type),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iconTint
                        )
                    }

                    // Timestamp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccessTime,
                            null,
                            tint = GrayMedium,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(text = timeLabel, fontSize = 11.sp, color = GrayMedium)
                    }
                }
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyNotificationsState(filter: NotificationFilter) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (filter == NotificationFilter.UNREAD) "No Unread Notifications"
                else "No Notifications Yet",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (filter == NotificationFilter.UNREAD)
                    "You're all caught up! ✓"
                else
                    "When workers apply or you receive\nratings, they'll appear here.",
                fontSize = 14.sp,
                color = GrayMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private data class NotificationStyle(
    val icon: ImageVector,
    val bg: Color,
    val tint: Color
)

private operator fun NotificationStyle.component1() = icon
private operator fun NotificationStyle.component2() = bg
private operator fun NotificationStyle.component3() = tint

private fun notificationStyle(type: NotificationType): NotificationStyle =
    when (type) {
        NotificationType.APPLICATION -> NotificationStyle(
            icon = Icons.Outlined.Assignment,
            bg   = Color(0xFFE3F2FD),
            tint = Color(0xFF1E88E5)
        )
        NotificationType.APPLICATION_ACCEPTED -> NotificationStyle(
            icon = Icons.Outlined.CheckCircle,
            bg   = Color(0xFFE8F5E9),
            tint = Color(0xFF43A047)
        )
        NotificationType.APPLICATION_REJECTED -> NotificationStyle(
            icon = Icons.Outlined.Cancel,
            bg   = Color(0xFFFFEBEE),
            tint = Color(0xFFE53935)
        )
        NotificationType.RATING -> NotificationStyle(
            icon = Icons.Outlined.Star,
            bg   = Color(0xFFFFF8E1),
            tint = Color(0xFFFFA000)
        )
    }

private fun typeLabel(type: NotificationType): String =
    when (type) {
        NotificationType.APPLICATION          -> "New Application"
        NotificationType.APPLICATION_ACCEPTED -> "Accepted"
        NotificationType.APPLICATION_REJECTED -> "Not Selected"
        NotificationType.RATING               -> "Review"
    }

private fun formatTimeAgo(isoTimestamp: String?): String {
    if (isoTimestamp == null) return "Just now"
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now     = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours   = ChronoUnit.HOURS.between(instant, now)
        val days    = ChronoUnit.DAYS.between(instant, now)
        when {
            minutes < 1   -> "Just now"
            minutes < 60  -> "${minutes}m ago"
            hours   < 24  -> "${hours}h ago"
            days    < 7   -> "${days}d ago"
            else -> DateTimeFormatter
                .ofPattern("MMM d")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }
    } catch (e: Exception) {
        "Recently"
    }
}