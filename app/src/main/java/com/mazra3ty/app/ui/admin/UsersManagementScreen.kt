package com.mazra3ty.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.GreenPrimaryDark
import com.mazra3ty.app.ui.theme.RedError
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.launch

// ─── Screen ──────────────────────────────────────────────────────────────────
suspend fun softDeleteUser(userId: String) {
    SupabaseClientProvider.client
        .from("users")
        .update(
            mapOf(
                "is_deleted" to true
            )
        ) {
            filter { eq("id", userId) }
        }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterRole by remember { mutableStateOf<String?>(null) } // null = all
    var showFilterMenu by remember { mutableStateOf(false) }

    // Delete dialog state
    var userToDelete by remember { mutableStateOf<User?>(null) }

    // ── Load users from Supabase ──────────────────────────────────────────────
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                users = SupabaseClientProvider.client
                    .postgrest["active_users"]
                    .select()
                    .decodeList<User>()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ── Filter logic ──────────────────────────────────────────────────────────
    val filteredUsers = users.filter { user ->
        val matchesSearch = user.full_name.contains(searchQuery, ignoreCase = true) ||
                user.email?.contains(searchQuery, ignoreCase = true) == true
        val matchesRole = filterRole == null || user.role == filterRole
        matchesSearch && matchesRole
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manager Users",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            // ── Search + Filter Row ───────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search..", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray)
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedContainerColor = Color(0xFFEEF7E8),
                        unfocusedContainerColor = Color(0xFFEEF7E8)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(52.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Filter button
                Box {
                    IconButton(
                        onClick = { showFilterMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEF7E8))
                            .border(1.dp, GreenPrimary, CircleShape)
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Filter", tint = GreenPrimary)
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        listOf(null to "All", "worker" to "Workers", "farmer" to "Farmers").forEach { (role, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    filterRole = role
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Content ───────────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                }
                filteredUsers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users found", color = Color.Gray)
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredUsers, key = { it.id }) { user ->
                            UserCard(
                                user = user,
                                onDelete = { userToDelete = user },
                                onToggleBan = { isBanned ->
                                    scope.launch {
                                        try {
                                            SupabaseClientProvider.client
                                                .postgrest["active_users"]
                                                .select()
                                                .decodeList<User>()
                                            // Update local list
                                            users = users.map {
                                                if (it.id == user.id) it.copy(is_banned = isBanned) else it
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ────────────────────────────────────────────
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete \"${user.full_name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                softDeleteUser(user.id)

                                // remove from UI (simulate deletion)
                                users = users.filter { it.id != user.id }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                userToDelete = null
                            }
                        }
                    }
                ) {
                    Text("Delete", color = RedError)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── User Card ────────────────────────────────────────────────────────────────

@Composable
private fun UserCard(
    user: User,
    onDelete: () -> Unit,
    onToggleBan: (Boolean) -> Unit
) {
    val isActive = !user.is_banned

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Top row: avatar + name + delete button ────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary.copy(alpha = 0.15f))
                        .border(2.dp, GreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = user.full_name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RedError.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = RedError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

            // ── Bottom row: email + role + toggle ─────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "email: ${user.email ?: "—"}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = user.role,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Active / Banned toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isActive) "Active" else "Banned",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) GreenPrimaryDark else RedError
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isActive,
                        onCheckedChange = { checked -> onToggleBan(!checked) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GreenPrimaryDark,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = RedError
                        )
                    )
                }
            }
        }
    }
}