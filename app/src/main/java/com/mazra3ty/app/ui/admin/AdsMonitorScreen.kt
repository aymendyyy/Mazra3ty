package com.mazra3ty.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Job
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.GreenPrimaryDark
import com.mazra3ty.app.ui.theme.RedError
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdsMonitorScreen(onBack: () -> Unit) {

    val scope = rememberCoroutineScope()

    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var users by remember { mutableStateOf<Map<String, String>>(emptyMap()) } // id → full_name
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var jobToDelete by remember { mutableStateOf<Job?>(null) }

    // ── Load ──────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val loadedJobs = SupabaseClientProvider.client
                    .postgrest["jobs"]
                    .select()
                    .decodeList<Job>()

                val loadedUsers = SupabaseClientProvider.client
                    .postgrest["users"]
                    .select()
                    .decodeList<User>()

                jobs = loadedJobs
                users = loadedUsers.associate { it.id to it.full_name }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    val filtered = jobs.filter { job ->
        val matchSearch = job.title.contains(searchQuery, ignoreCase = true) ||
                job.location?.contains(searchQuery, ignoreCase = true) == true
        val matchStatus = filterStatus == null || job.status == filterStatus
        matchSearch && matchStatus
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Manager jobs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
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

            // Search + Filter
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
                        listOf(null to "All", "open" to "Open", "closed" to "Closed").forEach { (s, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { filterStatus = s; showFilterMenu = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No jobs found", color = Color.Gray)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            farmerName = users[job.farmer_id] ?: "Unknown",
                            onDelete = { jobToDelete = job }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Delete dialog
    jobToDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            title = { Text("Delete Job") },
            text = { Text("Delete \"${job.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            SupabaseClientProvider.client
                                .postgrest["jobs"]
                                .delete { filter { eq("id", job.id) } }
                            jobs = jobs.filter { it.id != job.id }
                        } catch (e: Exception) { e.printStackTrace() }
                        finally { jobToDelete = null }
                    }
                }) { Text("Delete", color = RedError) }
            },
            dismissButton = {
                TextButton(onClick = { jobToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── Job Card ─────────────────────────────────────────────────────────────────

@Composable
private fun JobCard(job: Job, farmerName: String, onDelete: () -> Unit) {

    // Calculate days since created
    val daysAgo = remember(job.created_at) {
        try {
            val created = Instant.parse(job.created_at ?: return@remember null)
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

            // Top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
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

                // Title + salary
                Column(modifier = Modifier.weight(1f)) {
                    if (job.title.isNotBlank()) {
                        Text(job.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    job.salary?.let {
                        Text(
                            text = "${it.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GreenPrimaryDark
                        )
                        Text("DZD/J", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                // Delete button
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

            // Bottom row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    // Location
                    job.location?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(it, fontSize = 12.sp, color = Color.DarkGray)
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    // Farmer name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(farmerName, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // Days ago
                daysAgo?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (it == 0L) "Today" else "$it Days",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}