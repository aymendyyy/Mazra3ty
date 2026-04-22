package com.mazra3ty.app.ui.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.notifications.NotificationStore
import com.mazra3ty.app.notifications.PersistedNotification
import com.mazra3ty.app.ui.screens.LogoMark
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// ─── Icon resolver ────────────────────────────────────────────────────────────

private fun iconForKey(key: String): ImageVector = when (key) {
    "person_add" -> Icons.Outlined.PersonAdd
    "work"       -> Icons.Outlined.Work
    "star"       -> Icons.Outlined.Star
    "assignment" -> Icons.Outlined.Assignment
    else         -> Icons.Outlined.Notifications
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    currentRoute: String,
    onLogout:     () -> Unit,
    onNavigate:   (String) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── State ──────────────────────────────────────────────────────────────────
    var notifications   by remember { mutableStateOf(NotificationStore.load(context)) }
    var showNotifPanel  by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var adminUser       by remember { mutableStateOf<User?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Load admin user info once
    LaunchedEffect(Unit) {
        runCatching {
            val uid = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@runCatching
            adminUser = SupabaseClientProvider.client.postgrest["users"]
                .select { filter { eq("id", uid) } }
                .decodeSingle()
        }
    }

    // Refresh notification list whenever the panel opens
    LaunchedEffect(showNotifPanel) {
        if (showNotifPanel) notifications = NotificationStore.load(context)
    }

    // ── Title mapping ──────────────────────────────────────────────────────────
    val screenTitle = when {
        currentRoute.startsWith(AdminScreen.Dashboard.route)    -> "Dashboard"
        currentRoute.startsWith(AdminScreen.Users.route)        -> "Users"
        currentRoute.startsWith(AdminScreen.Ads.route)          -> "Jobs"
        currentRoute.startsWith(AdminScreen.Reviews.route)      -> "Reviews"
        currentRoute.startsWith(AdminScreen.Statistics.route)   -> "Statistics"
        currentRoute.startsWith(AdminScreen.Reports.route)      -> "Reports"
        currentRoute.startsWith(AdminScreen.Applications.route) -> "Applications"
        else                                                     -> "Admin Panel"
    }

    // ── Top App Bar ────────────────────────────────────────────────────────────
    TopAppBar(
        // ── Logo + title ──────────────────────────────────────────────────────
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoMark(32.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        screenTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Color(0xFF1A1A1A)
                    )
                    Text(
                        "Admin Panel",
                        fontSize = 11.sp,
                        color    = GreenPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        // ── Actions ───────────────────────────────────────────────────────────
        actions = {
            // Notification bell
            IconButton(onClick = { showNotifPanel = true }) {
                BadgedBox(
                    badge = {
                        if (notifications.isNotEmpty()) {
                            Badge(containerColor = RedError) {
                                Text(
                                    text     = if (notifications.size > 99) "99+" else notifications.size.toString(),
                                    fontSize = 9.sp,
                                    color    = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector        = if (notifications.isEmpty()) Icons.Outlined.NotificationsNone
                        else Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint               = if (notifications.isEmpty()) Color.Gray else GreenPrimary
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Profile avatar button → opens profile menu
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(GreenPrimary, GreenPrimaryDark))
                    )
                    .clickable { showProfileMenu = true },
                contentAlignment = Alignment.Center
            ) {
                val initials = adminUser?.full_name
                    ?.split(" ")
                    ?.take(2)
                    ?.joinToString("") { it.take(1).uppercase() }
                    ?: "A"
                Text(
                    text       = initials,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    //  NOTIFICATION PANEL
    // ─────────────────────────────────────────────────────────────────────────
    if (showNotifPanel) {
        Dialog(
            onDismissRequest = { showNotifPanel = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                // Dim background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { showNotifPanel = false }
                )

                // Panel
                Card(
                    shape    = RoundedCornerShape(bottomStart = 24.dp, topStart = 0.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.88f)
                        .shadow(24.dp, RoundedCornerShape(bottomStart = 24.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // ── Panel Header ──────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(listOf(GreenPrimary, GreenPrimaryDark))
                                )
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier              = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Notifications,
                                            null,
                                            tint     = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Notifications",
                                            color      = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 20.sp
                                        )
                                    }
                                    IconButton(
                                        onClick  = { showNotifPanel = false },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(Icons.Outlined.Close, null, tint = Color.White)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier              = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "${notifications.size} notification${if (notifications.size != 1) "s" else ""}",
                                            color    = Color.White,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (notifications.isNotEmpty()) {
                                        TextButton(
                                            onClick = { showClearDialog = true },
                                        ) {
                                            Icon(
                                                Icons.Outlined.DeleteSweep,
                                                null,
                                                tint     = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Clear all",
                                                color    = Color.White.copy(alpha = 0.85f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── List ──────────────────────────────────────────────
                        if (notifications.isEmpty()) {
                            Box(
                                modifier         = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.NotificationsNone,
                                        null,
                                        tint     = Color(0xFFDDDDDD),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "No notifications yet",
                                        color      = Color(0xFFAAAAAA),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 16.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "New activity will appear here",
                                        color    = Color(0xFFCCCCCC),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Swipe left to dismiss",
                                fontSize = 11.sp,
                                color    = Color(0xFFBBBBBB),
                                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp)
                            )
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(notifications, key = { it.id }) { notif ->
                                    SwipeToDeleteNotifItem(
                                        notif     = notif,
                                        onDelete  = {
                                            NotificationStore.deleteOne(context, notif.id)
                                            notifications = NotificationStore.load(context)
                                        },
                                        onClick   = {
                                            showNotifPanel = false
                                            onNavigate(notif.route)
                                        }
                                    )
                                }
                                item { Spacer(Modifier.height(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Clear-all confirmation ─────────────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon    = {
                Icon(Icons.Outlined.DeleteSweep, null, tint = RedError, modifier = Modifier.size(32.dp))
            },
            title   = { Text("Clear all notifications?") },
            text    = { Text("This will permanently remove all ${notifications.size} notifications. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        NotificationStore.clearAll(context)
                        notifications   = emptyList()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) { Text("Clear All", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PROFILE MENU DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    if (showProfileMenu) {
        Dialog(
            onDismissRequest = { showProfileMenu = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                // Dim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { showProfileMenu = false }
                )

                // Menu card
                Card(
                    shape  = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .wrapContentHeight()
                        .align(Alignment.TopEnd)
                        .shadow(20.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                ) {
                    Column {

                        // ── Admin identity banner ──────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF1A2A1A), Color(0xFF2D4A2D)))
                                )
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Large avatar
                                Box(
                                    modifier         = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(GreenPrimary, GreenPrimaryDark))
                                        )
                                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = adminUser?.full_name
                                        ?.split(" ")?.take(2)
                                        ?.joinToString("") { it.take(1).uppercase() }
                                        ?: "A"
                                    Text(
                                        initials,
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 22.sp
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column {
                                    Text(
                                        adminUser?.full_name ?: "Administrator",
                                        color      = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 16.sp
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    adminUser?.email?.let {
                                        Text(
                                            it,
                                            color    = Color.White.copy(alpha = 0.65f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = GreenPrimary.copy(alpha = 0.3f)
                                    ) {
                                        Row(
                                            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.AdminPanelSettings,
                                                null,
                                                tint     = GreenLight,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Administrator",
                                                color      = GreenLight,
                                                fontSize   = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Info rows ──────────────────────────────────────────
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

                            adminUser?.phone?.let { phone ->
                                ProfileInfoRow(Icons.Outlined.Phone, "Phone", phone)
                                HorizontalDivider(color = Color(0xFFF2F2F2), modifier = Modifier.padding(vertical = 8.dp))
                            }

                            adminUser?.created_at?.let { date ->
                                ProfileInfoRow(Icons.Outlined.CalendarToday, "Member since", date.take(10))
                                HorizontalDivider(color = Color(0xFFF2F2F2), modifier = Modifier.padding(vertical = 8.dp))
                            }

                            ProfileInfoRow(
                                icon  = Icons.Outlined.Security,
                                label = "Access level",
                                value = "Full admin access"
                            )
                        }

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        // ── Logout button ──────────────────────────────────────
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick  = {
                                    showProfileMenu = false
                                    onLogout()
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFFF4444), Color(0xFFCC0000))
                                            ),
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Logout,
                                            null,
                                            tint     = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Sign Out",
                                            color      = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ─── Profile info row ────────────────────────────────────────────────────────

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier         = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GreenPrimary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
        }
    }
}

// ─── Swipe-to-delete notification item ───────────────────────────────────────

@Composable
private fun SwipeToDeleteNotifItem(
    notif:    PersistedNotification,
    onDelete: () -> Unit,
    onClick:  () -> Unit
) {
    var offsetX       by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "swipe"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Delete background revealed by swipe
        Box(
            modifier         = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .background(RedError),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier          = Modifier.padding(end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // Notification card
        Card(
            shape    = RoundedCornerShape(14.dp),
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            border   = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(notif.iconColor).copy(alpha = 0.18f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = animatedOffset.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -80f) onDelete() else offsetX = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            offsetX = (offsetX + delta * 0.3f).coerceIn(-120f, 0f)
                        }
                    )
                }
                .clickable { onClick() }
        ) {
            Row(
                modifier          = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier         = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(notif.iconColor).copy(alpha = 0.12f))
                        .border(1.dp, Color(notif.iconColor).copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        iconForKey(notif.iconKey),
                        null,
                        tint     = Color(notif.iconColor),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        notif.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = Color(0xFF1A1A1A),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        notif.subtitle,
                        fontSize = 12.sp,
                        color    = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Time + arrow
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        notif.timeLabel,
                        fontSize = 10.sp,
                        color    = Color(0xFFBBBBBB)
                    )
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        Icons.Outlined.ChevronRight,
                        null,
                        tint     = Color(notif.iconColor).copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    HorizontalDivider(
        color    = Color(0xFFF5F5F5),
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}