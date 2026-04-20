package com.mazra3ty.app.ui.admin

import android.content.Intent

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.*
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

private const val PAGE_SIZE = 5

private enum class UserTab { ACTIVE, DELETED }

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementScreen(onBack: () -> Unit) {

    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    // ── Data ──────────────────────────────────────────────────────────────────
    var activeUsers  by remember { mutableStateOf<List<User>>(emptyList()) }
    var deletedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    // userId → image URL  (filled per batch so ALL users' images are visible)
    var userImages   by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // ── Pagination ────────────────────────────────────────────────────────────
    var activeOffset   by remember { mutableStateOf(0) }
    var deletedOffset  by remember { mutableStateOf(0) }
    var hasMoreActive  by remember { mutableStateOf(true) }
    var hasMoreDeleted by remember { mutableStateOf(true) }
    var isInitialLoad  by remember { mutableStateOf(true) }
    var isLoadingMore  by remember { mutableStateOf(false) }

    // ── UI State ──────────────────────────────────────────────────────────────
    var activeTab      by remember { mutableStateOf(UserTab.ACTIVE) }
    var searchQuery    by remember { mutableStateOf("") }
    var filterRole     by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var expandedId     by remember { mutableStateOf<String?>(null) }
    var selectedUser   by remember { mutableStateOf<User?>(null) }
    var userToDelete   by remember { mutableStateOf<User?>(null) }

    // ─────────────────────────────────────────────────────────────────────────
    // Image fetcher: explicitly queries by user IDs (bypasses session-based RLS)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun fetchImagesForBatch(batch: List<User>) {
        if (batch.isEmpty()) return
        try {
            val ids = batch.map { it.id }
            // Pass each id individually via OR to guarantee cross-account visibility
            val images = SupabaseClientProvider.client
                .postgrest["user_images"]
                .select {
                    filter {
                        isIn("user_id", ids)
                    }
                }
                .decodeList<UserImage>()

            // Keep only first image per user, merge into existing map
            val newEntries = images
                .groupBy { it.user_id }
                .mapValues { (_, list) -> list.first().image_url }

            userImages = userImages + newEntries
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Paginated loaders ─────────────────────────────────────────────────────
    suspend fun loadActivePage() {
        if (!hasMoreActive || isLoadingMore) return
        isLoadingMore = true
        try {
            val batch = SupabaseClientProvider.client
                .from("users")
                .select {
                    filter { eq("is_deleted", false) }
                    range(activeOffset.toLong(), (activeOffset + PAGE_SIZE - 1).toLong())
                }
                .decodeList<User>()

            fetchImagesForBatch(batch)
            activeUsers  = activeUsers + batch
            activeOffset += batch.size
            if (batch.size < PAGE_SIZE) hasMoreActive = false
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingMore = false
        }
    }

    suspend fun loadDeletedPage() {
        if (!hasMoreDeleted || isLoadingMore) return
        isLoadingMore = true
        try {
            val batch = SupabaseClientProvider.client
                .from("users")
                .select {
                    filter { eq("is_deleted", true) }
                    range(deletedOffset.toLong(), (deletedOffset + PAGE_SIZE - 1).toLong())
                }
                .decodeList<User>()

            fetchImagesForBatch(batch)
            deletedUsers  = deletedUsers + batch
            deletedOffset += batch.size
            if (batch.size < PAGE_SIZE) hasMoreDeleted = false
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingMore = false
        }
    }

    // ── Initial load ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        scope.launch {
            loadActivePage()
            loadDeletedPage()
            isInitialLoad = false
        }
    }

    // ── Client-side filter ────────────────────────────────────────────────────
    fun List<User>.filtered() = filter { u ->
        val q = searchQuery.trim()
        val matchSearch = q.isBlank()
                || u.full_name.contains(q, ignoreCase = true)
                || u.email?.contains(q, ignoreCase = true) == true
                || u.phone?.contains(q, ignoreCase = true) == true
        val matchRole = filterRole == null || u.role == filterRole
        matchSearch && matchRole
    }

    val filteredActive  = activeUsers.filtered()
    val filteredDeleted = deletedUsers.filtered()

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(containerColor = Color(0xFFF5F5F5)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(12.dp))

            // ── Tab pills ─────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                UserTab.entries.forEach { tab ->
                    val selected   = activeTab == tab
                    val isDeleted  = tab == UserTab.DELETED
                    val accent     = if (isDeleted) RedError else GreenPrimary
                    val (icon, label, count) = when (tab) {
                        UserTab.ACTIVE  -> Triple(Icons.Outlined.People,    "Active",  activeUsers.size)
                        UserTab.DELETED -> Triple(Icons.Outlined.PersonOff, "Deleted", deletedUsers.size)
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(40.dp))
                            .clickable { activeTab = tab; expandedId = null },
                        color           = if (selected) accent else Color.White,
                        shadowElevation = if (selected) 4.dp else 1.dp
                    ) {
                        Row(
                            verticalAlignment      = Alignment.CenterVertically,
                            horizontalArrangement  = Arrangement.spacedBy(6.dp),
                            modifier               = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(icon, null, Modifier.size(18.dp),
                                tint = if (selected) Color.White else Color.DarkGray)
                            Text(label, fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) Color.White else Color.DarkGray)
                            Surface(
                                color = if (selected) Color.White.copy(.25f) else Color.Gray.copy(.12f),
                                shape = CircleShape
                            ) {
                                Text(count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Search + filter ───────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value           = searchQuery,
                    onValueChange   = { searchQuery = it },
                    placeholder     = { Text("Search name, email, phone…", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon     = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    trailingIcon    = if (searchQuery.isNotBlank()) ({
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }) else null,
                    shape  = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = GreenPrimary,
                        unfocusedBorderColor    = Color(0xFFDDDDDD),
                        focusedContainerColor   = Color(0xFFEEF7E8),
                        unfocusedContainerColor = Color(0xFFEEF7E8)
                    ),
                    singleLine = true,
                    modifier   = Modifier.weight(1f).height(52.dp)
                )

                Spacer(Modifier.width(10.dp))

                Box {
                    IconButton(
                        onClick  = { showFilterMenu = true },
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(if (filterRole != null) GreenPrimary else Color(0xFFEEF7E8))
                            .border(1.dp, GreenPrimary, CircleShape)
                    ) {
                        Icon(Icons.Outlined.Tune, null,
                            tint = if (filterRole != null) Color.White else GreenPrimary)
                    }

                    DropdownMenu(
                        expanded          = showFilterMenu,
                        onDismissRequest  = { showFilterMenu = false }
                    ) {
                        listOf(
                            null     to "All Roles",
                            "worker" to "Workers",
                            "farmer" to "Farmers",
                            "admin"  to "Admins"
                        ).forEach { (role, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (filterRole == role)
                                            Icon(Icons.Outlined.Check, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                        else
                                            Spacer(Modifier.size(16.dp))
                                        Text(label)
                                    }
                                },
                                onClick = { filterRole = role; showFilterMenu = false }
                            )
                        }
                    }
                }
            }

            // Active role-filter chip
            AnimatedVisibility(
                visible = filterRole != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                filterRole?.let { role ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Filter:", fontSize = 12.sp, color = Color.Gray)
                        Surface(shape = RoundedCornerShape(20.dp), color = GreenPrimary.copy(.12f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(role.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { filterRole = null }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Outlined.Close, null, tint = GreenPrimary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Content ───────────────────────────────────────────────────
            when {
                isInitialLoad -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = GreenPrimary)
                        Text("Loading users…", color = Color.Gray, fontSize = 13.sp)
                    }
                }

                else -> {
                    val currentList = if (activeTab == UserTab.ACTIVE) filteredActive else filteredDeleted
                    val hasMore     = if (activeTab == UserTab.ACTIVE) hasMoreActive   else hasMoreDeleted
                    val isDelView   = activeTab == UserTab.DELETED

                    if (currentList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    if (searchQuery.isNotBlank()) Icons.Outlined.SearchOff else Icons.Outlined.People,
                                    null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    if (searchQuery.isNotBlank()) "No results found" else "No users here",
                                    color = Color.Gray, fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            items(currentList, key = { it.id }) { user ->
                                UserCard(
                                    user          = user,
                                    imageUrl      = userImages[user.id],
                                    isExpanded    = expandedId == user.id,
                                    isDeletedView = isDelView || user.is_deleted,
                                    onToggle      = { expandedId = if (expandedId == user.id) null else user.id },
                                    onViewProfile = { selectedUser = user },
                                    onDelete      = { userToDelete = user },
                                    onToggleBan   = { ban ->
                                        scope.launch {
                                            try {
                                                SupabaseClientProvider.client.from("users")
                                                    .update(mapOf(
                                                        "is_banned" to ban,
                                                        "banned_at" to if (ban) "now()" else null
                                                    )) { filter { eq("id", user.id) } }
                                                activeUsers = activeUsers.map {
                                                    if (it.id == user.id) it.copy(is_banned = ban) else it
                                                }
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }
                                    }
                                )
                            }

                            // ── Pagination footer ─────────────────────────
                            item {
                                Box(
                                    modifier          = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                    contentAlignment  = Alignment.Center
                                ) {
                                    when {
                                        isLoadingMore -> Row(
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color       = GreenPrimary,
                                                modifier    = Modifier.size(22.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text("Loading more…", color = Color.Gray, fontSize = 13.sp)
                                        }

                                        hasMore -> OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    if (activeTab == UserTab.ACTIVE) loadActivePage()
                                                    else loadDeletedPage()
                                                }
                                            },
                                            border = BorderStroke(1.5.dp, GreenPrimary),
                                            shape  = RoundedCornerShape(20.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
                                        ) {
                                            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Load next $PAGE_SIZE users", fontSize = 13.sp)
                                        }

                                        else -> Text(
                                            "All users loaded · ${currentList.size} total",
                                            fontSize = 12.sp, color = Color.LightGray
                                        )
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    // ── Profile bottom sheet ──────────────────────────────────────────────────
    selectedUser?.let { user ->
        UserProfileSheet(
            user        = user,
            imageUrl    = userImages[user.id],
            onDismiss   = { selectedUser = null },
            onDelete    = { userToDelete = user; selectedUser = null },
            onToggleBan = { ban ->
                scope.launch {
                    try {
                        SupabaseClientProvider.client.from("users")
                            .update(mapOf(
                                "is_banned" to ban,
                                "banned_at" to if (ban) "now()" else null
                            )) { filter { eq("id", user.id) } }

                        val updated = user.copy(is_banned = ban)
                        activeUsers  = activeUsers.map { if (it.id == user.id) updated else it }
                        selectedUser = updated
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        )
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon  = {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(RedError.copy(.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = RedError, modifier = Modifier.size(24.dp))
                }
            },
            title = { Text("Delete User", fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("You are about to delete:")
                    Spacer(Modifier.height(6.dp))
                    Text("\"${user.full_name}\"", fontWeight = FontWeight.SemiBold, color = RedError)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (user.is_deleted)
                            "This will permanently remove the user from the database."
                        else
                            "This will move the user to the deleted list.",
                        fontSize = 13.sp, color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                if (user.is_deleted) {
                                    SupabaseClientProvider.client.from("users")
                                        .delete { filter { eq("id", user.id) } }
                                    deletedUsers = deletedUsers.filter { it.id != user.id }
                                } else {
                                    SupabaseClientProvider.client.from("users")
                                        .update(mapOf("is_deleted" to true)) { filter { eq("id", user.id) } }
                                    activeUsers  = activeUsers.filter { it.id != user.id }
                                    deletedUsers = deletedUsers + user.copy(is_deleted = true)
                                }
                                if (expandedId == user.id) expandedId = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                userToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError),
                    shape  = RoundedCornerShape(12.dp)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { userToDelete = null },
                    border  = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// User Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UserCard(
    user: User,
    imageUrl: String?,
    isExpanded: Boolean,
    isDeletedView: Boolean,
    onToggle: () -> Unit,
    onViewProfile: () -> Unit,
    onDelete: () -> Unit,
    onToggleBan: (Boolean) -> Unit
) {
    val accent = when {
        isDeletedView  -> Color(0xFF9E9E9E)
        user.is_banned -> RedError
        else           -> GreenPrimary
    }
    val borderColor = when {
        isDeletedView  -> Color(0xFFBDBDBD).copy(.5f)
        user.is_banned -> RedError.copy(.4f)
        else           -> GreenPrimary.copy(.3f)
    }

    Card(
        shape   = RoundedCornerShape(16.dp),
        colors  = CardDefaults.cardColors(containerColor = Color.White),
        border  = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // ── Row header ────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(12.dp)
            ) {
                UserAvatar(name = user.full_name, imageUrl = imageUrl, sizeDp = 52, accent = accent)

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        user.full_name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        RoleBadge(role = user.role, accent = accent)
                        if (user.is_banned && !isDeletedView) BannedBadge()
                    }
                }

                // Open profile
                IconButton(onClick = onViewProfile, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.OpenInNew, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                }

                // Expand
                Icon(
                    if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null, tint = Color.Gray, modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(4.dp))

                // Delete
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(RedError.copy(.1f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = RedError, modifier = Modifier.size(18.dp))
                }
            }

            // ── Compact contact info ──────────────────────────────────────
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(8.dp))

                user.email?.let {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Email, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                        Text(it, fontSize = 12.sp, color = Color.DarkGray,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                user.phone?.let {
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Phone, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                        Text(it, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // ── Expanded details + ban toggle ─────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color    = accent.copy(.05f),
                    shape    = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {

                        user.date_of_birth?.let { DetailRow(Icons.Outlined.Cake,         "Date of Birth", it) }
                        user.bio?.let           { DetailRow(Icons.Outlined.Notes,         "Bio",           it.take(80) + if (it.length > 80) "…" else "") }
                        user.created_at?.let    { DetailRow(Icons.Outlined.CalendarToday, "Joined",        it.take(10)) }
                        user.banned_at?.let     { DetailRow(Icons.Outlined.Block,         "Banned at",     it.take(10)) }
                        user.banned_reason?.let { DetailRow(Icons.Outlined.Report,        "Ban reason",    it) }

                        if (!isDeletedView) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = accent.copy(.15f))
                            Spacer(Modifier.height(10.dp))

                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier              = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (user.is_banned) Icons.Outlined.LockOpen else Icons.Outlined.Block,
                                        null,
                                        tint     = if (user.is_banned) GreenPrimary else RedError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            if (user.is_banned) "Unban User" else "Ban User",
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color      = if (user.is_banned) GreenPrimary else RedError
                                        )
                                        Text(
                                            if (user.is_banned) "Restore access" else "Block from app",
                                            fontSize = 11.sp, color = Color.Gray
                                        )
                                    }
                                }
                                Switch(
                                    checked         = user.is_banned,
                                    onCheckedChange = { onToggleBan(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor   = Color.White,
                                        checkedTrackColor   = RedError,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFCCCCCC)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileSheet(
    user: User,
    imageUrl: String?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onToggleBan: (Boolean) -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor    = Color.White,
        shape             = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle        = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDDDDD))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {

            // ── Sheet header ──────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("User Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Cover banner ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFA5D66D))
                        )
                    )
            ) {
                Icon(
                    Icons.Outlined.Agriculture, null,
                    tint     = Color.White.copy(.15f),
                    modifier = Modifier.size(100.dp).align(Alignment.CenterEnd).offset(10.dp)
                )
            }

            // ── Avatar overlapping banner ─────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .offset(y = (-28).dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, Color.White, CircleShape)
                    ) {
                        UserAvatar(
                            name    = user.full_name,
                            imageUrl = imageUrl,
                            sizeDp  = 72,
                            accent  = GreenPrimary
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.padding(bottom = 4.dp).weight(1f)) {
                        Text(
                            user.full_name,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 19.sp,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            RoleBadge(user.role, GreenPrimary)
                            if (user.is_banned) BannedBadge()
                        }
                        user.created_at?.let {
                            Spacer(Modifier.height(3.dp))
                            Text("Joined ${it.take(10)}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // ── Contact buttons ───────────────────────────────────────────
            val hasPhone = !user.phone.isNullOrBlank()
            val hasEmail = !user.email.isNullOrBlank()

            if (hasPhone || hasEmail) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    if (hasPhone) {
                        ContactButton(
                            modifier = Modifier.weight(1f),
                            icon     = Icons.Outlined.Phone,
                            label    = "Call",
                            color    = GreenPrimaryDark
                        ) {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                            )
                        }
                    }
                    if (hasEmail) {
                        ContactButton(
                            modifier = Modifier.weight(1f),
                            icon     = Icons.Outlined.Email,
                            label    = "Email",
                            color    = Color(0xFF1E88E5)
                        ) {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${user.email}")
                            }
                            context.startActivity(intent)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            // ── Account details section ───────────────────────────────────
            ProfileSection(title = "Account Details") {
                user.email?.let       { ProfileInfoRow(Icons.Outlined.Email,          "Email",     it) }
                user.phone?.let       { ProfileInfoRow(Icons.Outlined.Phone,          "Phone",     it) }
                ProfileInfoRow(       Icons.Outlined.Badge,                            "Role",     user.role.replaceFirstChar { it.uppercase() })
                user.date_of_birth?.let { ProfileInfoRow(Icons.Outlined.Cake,         "Born",     it) }
                user.created_at?.let    { ProfileInfoRow(Icons.Outlined.CalendarToday,"Joined",   it.take(10)) }
                user.id.let             { ProfileInfoRow(Icons.Outlined.Fingerprint,  "User ID",  it.take(8) + "…") }
            }

            // ── Bio ───────────────────────────────────────────────────────
            user.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Spacer(Modifier.height(12.dp))
                ProfileSection(title = "Bio") {
                    Text(bio, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 22.sp)
                }
            }

            // ── Ban info ──────────────────────────────────────────────────
            if (user.is_banned) {
                Spacer(Modifier.height(12.dp))
                ProfileSection(title = "Ban Information") {
                    user.banned_at?.let     { ProfileInfoRow(Icons.Outlined.Block,  "Banned at", it.take(10)) }
                    user.banned_reason?.let { ProfileInfoRow(Icons.Outlined.Report, "Reason",    it) }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Admin actions ─────────────────────────────────────────────
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(14.dp))
            Text("Admin Actions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(10.dp))

            // Ban / Unban toggle
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (user.is_banned) GreenPrimary.copy(.07f) else RedError.copy(.07f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (user.is_banned) GreenPrimary.copy(.15f) else RedError.copy(.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (user.is_banned) Icons.Outlined.LockOpen else Icons.Outlined.Block,
                            null,
                            tint     = if (user.is_banned) GreenPrimary else RedError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            if (user.is_banned) "Unban User" else "Ban User",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            color      = if (user.is_banned) GreenPrimary else RedError
                        )
                        Text(
                            if (user.is_banned) "Restore access to the app" else "Block this user from the app",
                            fontSize = 11.sp, color = Color.Gray
                        )
                    }
                }
                Switch(
                    checked         = user.is_banned,
                    onCheckedChange = { onToggleBan(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = RedError,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GrayLight
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // Delete button
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RedError.copy(.07f))
                    .clickable { onDelete() }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RedError.copy(.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = RedError, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Delete User", fontWeight = FontWeight.SemiBold, color = RedError, fontSize = 14.sp)
                    Text(
                        if (user.is_deleted) "Permanently remove from database" else "Move to deleted users",
                        fontSize = 11.sp, color = Color.Gray
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = RedError.copy(.5f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shows the user's photo from Supabase Storage if available,
 * otherwise falls back to initials derived from [name].
 */
@Composable
private fun UserAvatar(name: String, imageUrl: String?, sizeDp: Int, accent: Color) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(accent.copy(.15f))
            .border(2.dp, accent.copy(.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model               = imageUrl,
                contentDescription  = "Avatar",
                modifier            = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            // Build initials: up to 2 chars from first words
            val initials = name.trim()
                .split(" ")
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
            Text(
                text       = initials,
                fontSize   = (sizeDp / 3).sp,
                fontWeight = FontWeight.Bold,
                color      = accent
            )
        }
    }
}

@Composable
private fun RoleBadge(role: String, accent: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = accent.copy(.12f)) {
        Text(
            role.replaceFirstChar { it.uppercase() },
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = accent,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun BannedBadge() {
    Surface(shape = RoundedCornerShape(20.dp), color = RedError.copy(.1f)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(Icons.Outlined.Block, null, tint = RedError, modifier = Modifier.size(10.dp))
            Text("Banned", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RedError)
        }
    }
}

@Composable
private fun ContactButton(
    modifier : Modifier = Modifier,
    icon     : ImageVector,
    label    : String,
    color    : Color,
    onClick  : () -> Unit
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(46.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Icon(icon, null, Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        border   = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(3.dp).height(13.dp).background(GreenPrimary, RoundedCornerShape(2.dp)))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(GreenPrimary.copy(.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, color = GrayMedium)
            Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(13.dp).padding(top = 1.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label: ", fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, color = Color(0xFF1A1A1A),
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}