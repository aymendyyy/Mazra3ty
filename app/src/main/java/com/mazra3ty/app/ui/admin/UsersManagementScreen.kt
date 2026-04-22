package com.mazra3ty.app.ui.admin

import androidx.compose.animation.AnimatedVisibility
import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Profile
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.GreenPrimaryDark
import com.mazra3ty.app.ui.theme.Mazra3tyTheme
import com.mazra3ty.app.ui.theme.RedError
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// ─── User section tabs ────────────────────────────────────────────────────────

private enum class UserTab { ACTIVE, DELETED }

@Serializable
data class AdminUserDto(
    val id: String,
    val full_name: String,
    val phone: String? = null,
    val role: String,
    val date_of_birth: String? = null,
    val is_banned: Boolean = false,
    val is_deleted: Boolean = false,
    val created_at: String? = null,
    val banned_at: String? = null,
    val banned_reason: String? = null,

    // from profile (flattened)
    val location: String? = null,
    val skills: List<String>? = null,
    val image_url: String? = null,
    val bio: String? = null
)
// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementScreen(onBack: () -> Unit) {

    val scope     = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current

    // ── Data ──────────────────────────────────────────────────────────────────
    var activeUsers  by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var deletedUsers by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }

    // ── UI ────────────────────────────────────────────────────────────────────
    var activeTab      by remember { mutableStateOf(UserTab.ACTIVE) }
    var searchQuery    by remember { mutableStateOf("") }
    var filterRole     by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var expandedUserId by remember { mutableStateOf<String?>(null) }
    var userToDelete   by remember { mutableStateOf<AdminUserDto?>(null) }

    // ── Load ──────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (isPreview) {
            isLoading = false
            return@LaunchedEffect
        }

        scope.launch {
            try {
                // Fetch all users with profiles joined
                val allUsers = SupabaseClientProvider.client
                    .from("user_with_profile")
                    .select()
                    .decodeList<AdminUserDto>()

                activeUsers = allUsers.filter { !it.is_deleted }
                deletedUsers = allUsers.filter { it.is_deleted }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ── Filtered lists ────────────────────────────────────────────────────────
    fun List<AdminUserDto>.applyFilters() = filter { user ->
        val matchSearch = user.full_name.contains(searchQuery, ignoreCase = true)
        val matchRole   = filterRole == null || user.role == filterRole
        matchSearch && matchRole
    }

    val filteredActive  = activeUsers.applyFilters()
    val filteredDeleted = deletedUsers.applyFilters()

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Tab chips ─────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                UserTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    val (icon, label, count) = when (tab) {
                        UserTab.ACTIVE  -> Triple(Icons.Outlined.People, "Active", activeUsers.size)
                        UserTab.DELETED -> Triple(Icons.Outlined.PersonOff, "Deleted", deletedUsers.size)
                    }
                    val isDeleted = tab == UserTab.DELETED

                    val backgroundColor = if (isSelected) {
                        if (isDeleted) RedError else GreenPrimary
                    } else {
                        Color.White.copy(alpha = 0.9f)
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(40.dp))
                            .clickable {
                                activeTab = tab
                                expandedUserId = null
                            },
                        color = backgroundColor,
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) Color.White else Color.DarkGray
                            )

                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.DarkGray
                            )

                            Surface(
                                color = if (isSelected)
                                    Color.White.copy(alpha = 0.25f)
                                else
                                    Color.Gray.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // ── Search + Role filter ──────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search..", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary, unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedContainerColor = Color(0xFFEEF7E8), unfocusedContainerColor = Color(0xFFEEF7E8)
                    ),
                    singleLine = true, modifier = Modifier.weight(1f).height(52.dp)
                )

                Spacer(Modifier.width(10.dp))

                Box {
                    IconButton(
                        onClick = { showFilterMenu = true },
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(Color(0xFFEEF7E8)).border(1.dp, GreenPrimary, CircleShape)
                    ) { Icon(Icons.Outlined.Tune, "Filter", tint = GreenPrimary) }

                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        listOf(null to "All", "worker" to "Workers", "farmer" to "Farmers")
                            .forEach { (role, label) ->
                                DropdownMenuItem(
                                    text    = { Text(label) },
                                    onClick = { filterRole = role; showFilterMenu = false }
                                )
                            }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Content ───────────────────────────────────────────────────
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }

                activeTab == UserTab.ACTIVE -> {
                    if (filteredActive.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No active users found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredActive, key = { it.id }) { user ->
                                UserCard(
                                    user        = user,
                                    isExpanded  = expandedUserId == user.id,
                                    isDeleted   = false,
                                    onToggle    = { expandedUserId = if (expandedUserId == user.id) null else user.id },
                                    onDelete    = { userToDelete = user },
                                    onToggleBan = { shouldBan ->
                                        scope.launch {
                                            try {
                                                SupabaseClientProvider.client
                                                    .from("users")
                                                    .update(mapOf("is_banned" to shouldBan,
                                                        "banned_at" to if (shouldBan) "now()" else null)) {
                                                        filter { eq("id", user.id) }
                                                    }
                                                activeUsers = activeUsers.map {
                                                    if (it.id == user.id) it.copy(is_banned = shouldBan) else it
                                                }
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }
                                    }
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }

                else -> {
                    if (filteredDeleted.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No deleted users found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredDeleted, key = { it.id }) { user ->
                                UserCard(
                                    user        = user,
                                    isExpanded  = expandedUserId == user.id,
                                    isDeleted   = true,
                                    onToggle    = { expandedUserId = if (expandedUserId == user.id) null else user.id },
                                    onDelete    = { userToDelete = user },
                                    onToggleBan = {}
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User") },
            text  = { Text("Permanently delete \"${user.full_name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            if (user.is_deleted) {
                                SupabaseClientProvider.client
                                    .from("users")
                                    .delete {
                                        filter { eq("id", user.id) }
                                    }
                                deletedUsers = deletedUsers.filter { it.id != user.id }
                            } else {
                                SupabaseClientProvider.client
                                    .from("users")
                                    .update(
                                        mapOf("is_deleted" to true)
                                    ) {
                                        filter { eq("id", user.id) }
                                    }
                                activeUsers = activeUsers.filter { it.id != user.id }
                                deletedUsers = deletedUsers + user.copy(is_deleted = true)
                            }
                            if (expandedUserId == user.id) expandedUserId = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            userToDelete = null
                        }
                    }
                }) {
                    Text("Delete", color = RedError)
                }
            },
            dismissButton = {
                    TextButton(onClick = { userToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── User Card ────────────────────────────────────────────────────────────────

@Composable
private fun UserCard(
    user: AdminUserDto,
    isExpanded: Boolean,
    isDeleted: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onToggleBan: (Boolean) -> Unit
) {
    val isActive    = !user.is_banned
    val accentColor = if (isDeleted) Color(0xFF9E9E9E) else GreenPrimary
    val imageUrl = user.image_url

    val borderColor = when {
        isDeleted -> Color(0xFFBDBDBD)
        !isActive -> RedError.copy(alpha = 0.5f)
        else      -> GreenPrimary.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {

            // 🔹 HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(12.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // ✅ AVATAR
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(2.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!imageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "User Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = "Default Avatar",
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // 🔹 USER INFO
                    Column(Modifier.weight(1f)) {
                        Text(
                            user.full_name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            user.role.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = accentColor
                        )
                    }

                    // 🔹 EXPAND BUTTON
                    Icon(
                        if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(4.dp))

                    // 🔹 DELETE BUTTON
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(RedError.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = RedError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color(0xFFEEEEEE)
                )

                // 🔹 STATUS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            user.phone ?: "No phone",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }

                    if (isDeleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEEEEE)
                        ) {
                            Text(
                                "Deleted",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 🔹 EXPANDABLE DETAILS
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                UserDetailBanner(
                    user = user,
                    accentColor = accentColor
                )
            }
        }
    }
}

// ─── Expandable detail banner ─────────────────────────────────────────────────

@Composable
private fun UserDetailBanner(user: AdminUserDto, accentColor: Color) {
    val imageUrl = user?.image_url
    Surface(
        color    = accentColor.copy(alpha = 0.06f),
        shape    = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = accentColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("Full Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = accentColor.copy(alpha = 0.15f))

            // Profile picture indicator
            if (imageUrl != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Icon(Icons.Outlined.Image, null, tint = accentColor, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Profile photo: ", fontSize = 12.sp, color = Color.Gray)
                    Text("Available", fontSize = 12.sp, color = GreenPrimaryDark, fontWeight = FontWeight.Medium)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Icon(Icons.Outlined.HideImage, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("No profile photo", fontSize = 12.sp, color = Color.Gray)
                }
            }

            HorizontalDivider(color = accentColor.copy(alpha = 0.10f), modifier = Modifier.padding(bottom = 8.dp))

            // All user fields
            UserBannerRow(Icons.Outlined.Badge,          "User ID",         user.id)
            UserBannerRow(Icons.Outlined.Person,         "Full name",       user.full_name)
            user.phone?.let        { UserBannerRow(Icons.Outlined.Phone,         "Phone",           it) }
            UserBannerRow(Icons.Outlined.ManageAccounts, "Role",            user.role.replaceFirstChar { it.uppercase() })
            user.date_of_birth?.let{ UserBannerRow(Icons.Outlined.Cake,          "Date of birth",   it) }
            user?.bio?.let{ UserBannerRow(Icons.Outlined.Notes,         "Bio",             it) }
            user.created_at?.let   { UserBannerRow(Icons.Outlined.CalendarMonth,  "Joined",          it.take(10)) }
            user.banned_at?.let    { UserBannerRow(Icons.Outlined.Block,          "Banned at",       it.take(10)) }
            user.banned_reason?.let{ UserBannerRow(Icons.Outlined.Report,         "Ban reason",      it) }

            Spacer(Modifier.height(8.dp))

            // Status badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserStatusBadge(
                    label = if (user.is_banned) "Banned" else "Active",
                    color = if (user.is_banned) RedError else GreenPrimaryDark
                )
                if (user.is_deleted) {
                    UserStatusBadge(label = "Deleted", color = Color(0xFF757575))
                }
            }
        }
    }
}

// ─── Small shared composables ─────────────────────────────────────────────────

@Composable
private fun UserBannerRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(13.dp).padding(top = 1.dp))
        Spacer(Modifier.width(5.dp))
        Text("$label: ", fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun UserStatusBadge(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.12f)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun UsersManagementPreview() {
    Mazra3tyTheme { UsersManagementScreen(onBack = {}) }
}
