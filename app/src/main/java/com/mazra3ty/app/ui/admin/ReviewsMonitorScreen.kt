package com.mazra3ty.app.ui.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Review
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.RedError
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsMonitorScreen(onBack: () -> Unit) {

    val scope = rememberCoroutineScope()

    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var users by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var starFilter by remember { mutableStateOf<Int?>(null) } // null = All
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
                users = loadedUsers.associateBy { it.id }
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
        val matchSearch = searchQuery.isBlank() ||
                reviewedUser?.full_name?.contains(searchQuery, ignoreCase = true) == true ||
                reviewerUser?.full_name?.contains(searchQuery, ignoreCase = true) == true ||
                review.comment?.contains(searchQuery, ignoreCase = true) == true
        val matchStar = starFilter == null || review.rating == starFilter
        matchSearch && matchStar
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ratings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Search Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search..", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
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
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = { /* optional extra filter */ },
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

            // Star filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(null to "All", 5 to "5 stars", 4 to "4 stars", 3 to "3 stars", 2 to "2 stars")
                items(filters) { (stars, label) ->
                    val selected = starFilter == stars
                    FilterChip(
                        selected = selected,
                        onClick = { starFilter = stars },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color.DarkGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            selectedBorderColor = GreenPrimary,
                            borderColor = Color(0xFFDDDDDD)
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

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
                            review = review,
                            reviewedUser = users[review.reviewed_id],
                            reviewerUser = users[review.reviewer_id],
                            onDelete = { reviewToDelete = review }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Delete dialog
    reviewToDelete?.let { review ->
        AlertDialog(
            onDismissRequest = { reviewToDelete = null },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete this review?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            SupabaseClientProvider.client
                                .postgrest["reviews"]
                                .delete { filter { eq("id", review.id) } }
                            reviews = reviews.filter { it.id != review.id }
                        } catch (e: Exception) { e.printStackTrace() }
                        finally { reviewToDelete = null }
                    }
                }) { Text("Delete", color = RedError) }
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
    review: Review,
    reviewedUser: User?,
    reviewerUser: User?,
    onDelete: () -> Unit
) {
    val daysAgo = remember(review.created_at) {
        try {
            val created = Instant.parse(review.created_at ?: return@remember null)
            ChronoUnit.DAYS.between(created, Instant.now())
        } catch (e: Exception) { null }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Top: reviewed user avatar + name + role + delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
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
                        text = reviewedUser?.full_name ?: "Unknown",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = reviewedUser?.role?.replaceFirstChar { it.uppercase() } ?: "",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RedError.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = RedError, modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

            // Top review label + stars + date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Top Review", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                Spacer(Modifier.width(6.dp))
                StarRow(rating = review.rating)
                Spacer(Modifier.width(6.dp))
                daysAgo?.let {
                    Text(
                        text = if (it == 0L) "Today" else "${it}d ago",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Reviewer mini row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary.copy(alpha = 0.1f))
                        .border(1.dp, GreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = reviewerUser?.full_name ?: "Unknown",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    review.comment?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarRow(rating = review.rating, size = 11)
                            Spacer(Modifier.width(4.dp))
                            Text(it, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ─── Star Row ─────────────────────────────────────────────────────────────────

@Composable
private fun StarRow(rating: Int, size: Int = 14) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(size.dp)
            )
        }
    }
}