package com.mazra3ty.app.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Review
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.GreenPrimaryDark
import com.mazra3ty.app.ui.theme.RedError
import com.mazra3ty.app.ui.theme.TextPrimary
import com.mazra3ty.app.ui.theme.GrayDark
import com.mazra3ty.app.ui.theme.GrayLight
import com.mazra3ty.app.ui.theme.SoftGreenBg
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsMonitorScreen(onBack: () -> Unit) {

    val scope = rememberCoroutineScope()

    var reviews       by remember { mutableStateOf<List<Review>>(emptyList()) }
    var users         by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var isLoading     by remember { mutableStateOf(true) }
    var searchQuery   by remember { mutableStateOf("") }
    var starFilter    by remember { mutableStateOf<Int?>(null) }
    var reviewToDelete by remember { mutableStateOf<Review?>(null) }

    // ── Load ──────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val loadedReviews = SupabaseClientProvider.client
                    .postgrest["reviews"]
                    .select()
                    .decodeList<Review>()

                val loadedUsers = SupabaseClientProvider.client
                    .postgrest["users"]
                    .select()
                    .decodeList<User>()

                reviews = loadedReviews
                users   = loadedUsers.associateBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    val filtered = reviews.filter { review ->
        val reviewedUser = users[review.reviewed_id]
        val reviewerUser = users[review.reviewer_id]
        val matchSearch  = searchQuery.isBlank() ||
                reviewedUser?.full_name?.contains(searchQuery, ignoreCase = true) == true ||
                reviewerUser?.full_name?.contains(searchQuery, ignoreCase = true) == true ||
                review.comment?.contains(searchQuery, ignoreCase = true) == true
        val matchStar    = starFilter == null || review.rating == starFilter
        matchSearch && matchStar
    }

    // ── UI ────────────────────────────────────────────────────────────────────
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

            // ── Search Row ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search by name or comment…", color = Color.Gray) },
                    leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    shape         = RoundedCornerShape(24.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = GreenPrimary,
                        unfocusedBorderColor    = Color(0xFFDDDDDD),
                        focusedContainerColor   = Color(0xFFEEF7E8),
                        unfocusedContainerColor = Color(0xFFEEF7E8)
                    ),
                    singleLine = true,
                    modifier   = Modifier.weight(1f).height(52.dp)
                )
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick  = { /* no extra filter needed */ },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF7E8))
                        .border(1.dp, GreenPrimary, CircleShape)
                ) {
                    Icon(Icons.Outlined.Tune, null, tint = GreenPrimary)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Star filter chips ─────────────────────────────────────────
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(null to "All", 5 to "5 ★", 4 to "4 ★", 3 to "3 ★", 2 to "2 ★", 1 to "1 ★")
                items(filters) { (stars, label) ->
                    val selected = starFilter == stars
                    FilterChip(
                        selected = selected,
                        onClick  = { starFilter = stars },
                        label    = { Text(label, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor     = Color.White,
                            containerColor         = Color.White,
                            labelColor             = Color.DarkGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = selected,
                            selectedBorderColor = GreenPrimary,
                            borderColor         = Color(0xFFDDDDDD)
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Content ───────────────────────────────────────────────────
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No reviews found", color = Color.Gray)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered, key = { it.id }) { review ->
                        ReviewCard(
                            review       = review,
                            reviewedUser = users[review.reviewed_id],
                            reviewerUser = users[review.reviewer_id],
                            onDelete     = { reviewToDelete = review }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // ── Delete confirmation dialog (permanent hard-delete) ────────────────────
    reviewToDelete?.let { review ->
        val reviewedName = users[review.reviewed_id]?.full_name ?: "this user"
        AlertDialog(
            onDismissRequest = { reviewToDelete = null },
            icon  = { Icon(Icons.Outlined.DeleteForever, null, tint = RedError) },
            title = { Text("Delete Review", fontWeight = FontWeight.SemiBold) },
            text  = {
                Text(
                    "Permanently delete the ${review.rating}-star review about \"$reviewedName\"?\n" +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            // Hard delete — permanently removes the row from the database
                            SupabaseClientProvider.client
                                .postgrest["reviews"]
                                .delete { filter { eq("id", review.id) } }
                            reviews = reviews.filter { it.id != review.id }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            reviewToDelete = null
                        }
                    }
                }) { Text("Delete permanently", color = RedError, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { reviewToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── Review Card ──────────────────────────────────────────────────────────────

@Composable
private fun ReviewCard(
    review:       Review,
    reviewedUser: User?,
    reviewerUser: User?,
    onDelete:     () -> Unit
) {
    // Track whether the "reviewed user detail" panel is expanded
    var expanded by remember { mutableStateOf(false) }

    val daysAgo = remember(review.created_at) {
        try {
            val created = Instant.parse(review.created_at ?: return@remember null)
            ChronoUnit.DAYS.between(created, Instant.now())
        } catch (e: Exception) { null }
    }

    // Rating colour
    val ratingColor = when {
        review.rating >= 4 -> GreenPrimaryDark
        review.rating == 3 -> Color(0xFFFB8C00)
        else               -> RedError
    }

    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Header: reviewed-user avatar / name / role + delete ────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }   // tap header to expand
            ) {
                // Avatar
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary.copy(alpha = 0.15f))
                        .border(2.dp, GreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, null, tint = GreenPrimary, modifier = Modifier.size(30.dp))
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = reviewedUser?.full_name ?: "Unknown",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        color      = TextPrimary
                    )
                    // Role chip
                    reviewedUser?.role?.let { role ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (role) {
                                "farmer" -> Color(0xFFFFF3E0)
                                "worker" -> Color(0xFFE8F5E9)
                                else     -> GrayLight
                            }
                        ) {
                            Text(
                                text     = role.replaceFirstChar { it.uppercase() },
                                fontSize = 10.sp,
                                color    = when (role) {
                                    "farmer" -> Color(0xFFFB8C00)
                                    "worker" -> GreenPrimaryDark
                                    else     -> GrayDark
                                },
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Expand chevron
                Icon(
                    imageVector        = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint               = GrayDark,
                    modifier           = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(4.dp))

                // Delete button
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RedError.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = RedError, modifier = Modifier.size(20.dp))
                }
            }

            // ── Expandable: reviewed-user detail panel ────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftGreenBg.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "Reviewed User Details",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = GreenPrimaryDark
                            )
                            Spacer(Modifier.height(6.dp))
                            reviewedUser?.let { u ->
                                DetailRow(Icons.Outlined.Email,       u.email  ?: "—")
                                DetailRow(Icons.Outlined.Phone,       u.phone  ?: "—")
                                DetailRow(Icons.Outlined.Cake,        u.date_of_birth ?: "—")
                                DetailRow(Icons.Outlined.Notes,       u.bio    ?: "—")
                                DetailRow(
                                    Icons.Outlined.AccessTime,
                                    if (u.created_at != null) formatRelative(u.created_at) else "—",
                                    label = "Joined"
                                )
                                if (u.is_banned) {
                                    Spacer(Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = RedError.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.Block, null, tint = RedError, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Banned", fontSize = 11.sp, color = RedError, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            } ?: Text("User data unavailable", fontSize = 12.sp, color = GrayDark)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

            // ── Review body: stars + date + rating badge ──────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRow(rating = review.rating)
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ratingColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${review.rating}/5",
                        fontSize   = 11.sp,
                        color      = ratingColor,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                daysAgo?.let {
                    Text(
                        text     = if (it == 0L) "Today" else "${it}d ago",
                        fontSize = 11.sp,
                        color    = Color.Gray
                    )
                }
            }

            review.comment?.let { comment ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = "\"$comment\"",
                    fontSize = 13.sp,
                    color    = Color.DarkGray
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Reviewer mini row ─────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary.copy(alpha = 0.1f))
                        .border(1.dp, GreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, null, tint = GreenPrimary, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text     = "Reviewed by: ${reviewerUser?.full_name ?: "Unknown"}",
                    fontSize = 11.sp,
                    color    = GrayDark
                )
                reviewerUser?.role?.let { role ->
                    Spacer(Modifier.width(4.dp))
                    Text("· $role", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ─── Detail Row ───────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(icon: ImageVector, value: String, label: String? = null) {
    if (value == "—" && label == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(icon, null, tint = GreenPrimaryDark, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        if (label != null) {
            Text("$label: ", fontSize = 11.sp, color = GrayDark, fontWeight = FontWeight.Medium)
        }
        Text(value, fontSize = 11.sp, color = Color.DarkGray)
    }
}

// ─── Star Row ─────────────────────────────────────────────────────────────────

@Composable
private fun StarRow(rating: Int, size: Int = 16) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector        = if (index < rating) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint               = Color(0xFFFFC107),
                modifier           = Modifier.size(size.dp)
            )
        }
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun formatRelative(createdAt: String): String {
    return try {
        val instant = Instant.parse(createdAt)
        val days  = ChronoUnit.DAYS.between(instant, Instant.now())
        when {
            days > 30 -> "${days / 30}mo ago"
            days > 0  -> "${days}d ago"
            else      -> "Today"
        }
    } catch (e: Exception) { "—" }
}