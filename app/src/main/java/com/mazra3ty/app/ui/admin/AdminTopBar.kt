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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.database.types.UserWithProfile
import com.mazra3ty.app.database.types.Notification
import com.mazra3ty.app.notifications.NotificationStore
import com.mazra3ty.app.notifications.PersistedNotification
import com.mazra3ty.app.ui.screens.LogoMark
import com.mazra3ty.app.ui.theme.*
import com.mazra3ty.app.database.types.UserImage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

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

    var notifications   by remember { mutableStateOf(NotificationStore.load(context)) }
    var showNotifPanel  by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var adminUser       by remember { mutableStateOf<UserWithProfile?>(null) }
    // دالة مساعدة للحصول على الرابط العام للصورة
    val getPublicUrl = remember {
        { path: String? ->
            if (path.isNullOrBlank()) null
            else if (path.startsWith("http")) path
            else {
                // استبدل PROJECT_ID و BUCKET_NAME ببياناتك إذا لزم الأمر
                // عادة ما يكون التنسيق كالتالي في Supabase
                val projectId = "xojfclmshvsqpuvogvuo" // سأحاول استنتاجه أو يمكنك تزويدي به
                "https://$projectId.supabase.co/storage/v1/object/public/avatars/$path"
            }
        }
    }

    LaunchedEffect(Unit) {
        AdminNotificationWorker.schedule(context)
        runCatching {
            val uid = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@runCatching
            
            // Fetch user and profile (profile contains image_url)
            adminUser = SupabaseClientProvider.client.postgrest["users"]
                .select(Columns.raw("*, profiles(*)")) {
                    filter { eq("id", uid) }
                }
                .decodeSingle<UserWithProfile>()
        }
    }

    LaunchedEffect(showNotifPanel) {
        if (showNotifPanel) {
            notifications = NotificationStore.load(context)
            
            val prefs = context.getSharedPreferences(AdminNotificationWorker.PREFS_STAMP, android.content.Context.MODE_PRIVATE)
            val lastCheck = prefs.getString(AdminNotificationWorker.KEY_LAST_CHECK, null)
                ?: Instant.now().minus(24, ChronoUnit.HOURS).toString()

            scope.launch {
                runCatching {
                    val dbNotifs = SupabaseClientProvider.client.postgrest["notifications"]
                        .select {
                            filter { gte("created_at", lastCheck) }
                            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(50)
                        }
                        .decodeList<Notification>()
                    
                    val converted = dbNotifs.map { db ->
                        PersistedNotification(
                            id = "db_${db.id}",
                            iconKey = when(db.type) {
                                "APPLICATION" -> "assignment"
                                "RATING" -> "star"
                                else -> "notifications"
                            },
                            iconColor = 0xFF4CAF50L,
                            title = db.title,
                            subtitle = db.message,
                            route = when(db.type) {
                                "APPLICATION" -> "admin_ads" 
                                "RATING" -> "admin_reviews"
                                else -> "admin_dashboard"
                            },
                            timeLabel = "Live",
                            timestamp = db.created_at ?: ""
                        )
                    }
                    
                    if (converted.isNotEmpty()) {
                        NotificationStore.addAll(context, converted)
                        notifications = NotificationStore.load(context)
                    }
                }
            }
        }
    }

    val screenTitle = when {
        currentRoute.startsWith(AdminScreen.Dashboard.route)  -> "Dashboard"
        currentRoute.startsWith(AdminScreen.Users.route)      -> "Users"
        currentRoute.startsWith(AdminScreen.Ads.route)        -> "Jobs"
        currentRoute.startsWith(AdminScreen.Reviews.route)    -> "Reviews"
        currentRoute.startsWith(AdminScreen.Statistics.route) -> "Statistics"
        currentRoute.startsWith(AdminScreen.Reports.route)    -> "Reports"
        else                                                   -> "Admin Panel"
    }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoMark(32.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(screenTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
                    Text("Admin Panel", fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Medium)
                }
            }
        },
        actions = {
            IconButton(onClick = { showNotifPanel = true }) {
                BadgedBox(
                    badge = {
                        if (notifications.isNotEmpty()) {
                            Badge(containerColor = RedError) {
                                Text(
                                    text       = if (notifications.size > 99) "99+" else notifications.size.toString(),
                                    fontSize   = 9.sp,
                                    color      = Color.White,
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

            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GreenPrimary, GreenPrimaryDark)))
                    .clickable { showProfileMenu = true },
                contentAlignment = Alignment.Center
            ) {
                // Using image_url from profiles table
                val finalImageUrl = getPublicUrl(adminUser?.image_url)
                if (finalImageUrl != null) {
                    AsyncImage(
                        model = finalImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = { println("AdminTopBar: Error loading image from $finalImageUrl: ${it.result.throwable}") }
                    )
                } else {
                    val initials = adminUser?.full_name
                        ?.split(" ")?.filter { it.isNotBlank() }?.take(2)
                        ?.joinToString("") { it.take(1).uppercase() }
                        ?: "A"
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )

    if (showNotifPanel) {
        Dialog(
            onDismissRequest = { showNotifPanel = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable { showNotifPanel = false })
                Card(
                    shape    = RoundedCornerShape(bottomStart = 24.dp, topStart = 0.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.88f).shadow(24.dp, RoundedCornerShape(bottomStart = 24.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(GreenPrimary, GreenPrimaryDark))).padding(horizontal = 20.dp, vertical = 18.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Notifications, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text("Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    }
                                    IconButton(onClick = { showNotifPanel = false }, modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) {
                                        Icon(Icons.Outlined.Close, null, tint = Color.White)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.2f)) {
                                        Text("${notifications.size} notification${if (notifications.size != 1) "s" else ""}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                    }
                                    if (notifications.isNotEmpty()) {
                                        TextButton(onClick = { showClearDialog = true }) {
                                            Icon(Icons.Outlined.DeleteSweep, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Clear all", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (notifications.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.NotificationsNone, null, tint = Color(0xFFDDDDDD), modifier = Modifier.size(72.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("No notifications yet", color = Color(0xFFAAAAAA), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text("New activity will appear here", color = Color(0xFFCCCCCC), fontSize = 13.sp)
                                }
                            }
                        } else {
                            Text("Swipe left to dismiss", fontSize = 11.sp, color = Color(0xFFBBBBBB), modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp))
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(notifications, key = { it.id }) { notif ->
                                    SwipeToDeleteNotifItem(
                                        notif    = notif,
                                        onDelete = {
                                            NotificationStore.deleteOne(context, notif.id)
                                            notifications = NotificationStore.load(context)
                                        },
                                        onClick  = {
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

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon    = { Icon(Icons.Outlined.DeleteSweep, null, tint = RedError, modifier = Modifier.size(32.dp)) },
            title   = { Text("Clear all notifications?") },
            text    = { Text("This will permanently remove all notifications. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        NotificationStore.clearAll(context)
                        // تحديث طابع زمن المسح لمنع عودة الإشعارات القديمة من السيرفر
                        context.getSharedPreferences(AdminNotificationWorker.PREFS_STAMP, android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString(AdminNotificationWorker.KEY_LAST_CHECK, Instant.now().toString())
                            .apply()
                            
                        notifications = emptyList()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) { Text("Clear All", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    if (showProfileMenu) {
        Dialog(
            onDismissRequest = { showProfileMenu = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable { showProfileMenu = false })
                Card(
                    shape  = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(0.78f).wrapContentHeight().align(Alignment.TopEnd).shadow(20.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF1A2A1A), Color(0xFF2D4A2D)))).padding(horizontal = 20.dp, vertical = 24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Brush.linearGradient(listOf(GreenPrimary, GreenPrimaryDark))).border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                    val finalImageUrl = getPublicUrl(adminUser?.image_url)
                                    if (finalImageUrl != null) {
                                        AsyncImage(
                                            model = finalImageUrl, 
                                            contentDescription = null, 
                                            modifier = Modifier.fillMaxSize().clip(CircleShape), 
                                            contentScale = ContentScale.Crop,
                                            onError = { println("AdminProfileMenu: Error loading image from $finalImageUrl: ${it.result.throwable}") }
                                        )
                                    } else {
                                        val initials = adminUser?.full_name?.split(" ")?.filter { it.isNotBlank() }?.take(2)?.joinToString("") { it.take(1).uppercase() } ?: "A"
                                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(adminUser?.full_name ?: "Administrator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    adminUser?.phone?.let { Text(it, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp) }
                                    Spacer(Modifier.height(8.dp))
                                    Surface(shape = RoundedCornerShape(20.dp), color = GreenPrimary.copy(alpha = 0.3f)) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.AdminPanelSettings, null, tint = GreenLight, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Administrator", color = GreenLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            adminUser?.phone?.let { ProfileInfoRow(Icons.Outlined.Phone, "Phone", it) }
                            adminUser?.created_at?.let { ProfileInfoRow(Icons.Outlined.CalendarToday, "Member since", it.take(10)) }
                            ProfileInfoRow(Icons.Outlined.Security, "Access level", "Full admin access")
                        }
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(onClick = { showProfileMenu = false; onLogout() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(0.dp)) {
                                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFFF4444), Color(0xFFCC0000))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Logout, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(GreenPrimary.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
        }
    }
}

@Composable
private fun SwipeToDeleteNotifItem(notif: PersistedNotification, onDelete: () -> Unit, onClick: () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "swipe")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).background(RedError), contentAlignment = Alignment.CenterEnd) {
            Row(modifier = Modifier.padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(notif.iconColor).copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth().offset(x = animatedOffset.dp).pointerInput(Unit) {
                    detectHorizontalDragGestures(onDragEnd = { if (offsetX < -80f) onDelete() else offsetX = 0f }, onHorizontalDrag = { _, delta -> offsetX = (offsetX + delta * 0.3f).coerceIn(-120f, 0f) })
                }.clickable { onClick() }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(notif.iconColor).copy(alpha = 0.12f)).border(1.dp, Color(notif.iconColor).copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(iconForKey(notif.iconKey), null, tint = Color(notif.iconColor), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(notif.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1A1A1A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(notif.subtitle, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(notif.timeLabel, fontSize = 10.sp, color = Color(0xFFBBBBBB))
                    Icon(Icons.Outlined.ChevronRight, null, tint = Color(notif.iconColor).copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
