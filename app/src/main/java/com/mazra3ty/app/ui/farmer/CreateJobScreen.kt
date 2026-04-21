package com.mazra3ty.app.ui.farmer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.CreateJob
import com.mazra3ty.app.database.types.Job
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerJobsScreen(
    farmerId: String,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // ── State ─────────────────────────────────────────────────────────────────
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Dialog states
    var showCreateSheet by remember { mutableStateOf(false) }
    var jobToEdit by remember { mutableStateOf<Job?>(null) }
    var jobToDelete by remember { mutableStateOf<Job?>(null) }

    // ── Load ──────────────────────────────────────────────────────────────────
    fun loadJobs() {
        scope.launch {
            isLoading = true
            try {
                jobs = SupabaseClientProvider.client
                    .postgrest["jobs"]
                    .select { filter { eq("farmer_id", farmerId) } }
                    .decodeList<Job>()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(farmerId) { loadJobs() }

    // ── Filtered list ─────────────────────────────────────────────────────────
    val filtered = jobs.filter { job ->
        val matchSearch = job.title.contains(searchQuery, ignoreCase = true) ||
                job.location?.contains(searchQuery, ignoreCase = true) == true ||
                job.description.contains(searchQuery, ignoreCase = true)
        val matchStatus = filterStatus == null || job.status == filterStatus
        matchSearch && matchStatus
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    val openCount   = jobs.count { it.status == "open" }
    val closedCount = jobs.count { it.status == "closed" }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Job Posts", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("${jobs.size} total", fontSize = 12.sp, color = GreenPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "New Job")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Summary chips ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    label = "Open",
                    count = openCount,
                    color = GreenPrimary,
                    selected = filterStatus == "open",
                    onClick = { filterStatus = if (filterStatus == "open") null else "open" }
                )
                StatChip(
                    label = "Closed",
                    count = closedCount,
                    color = Color(0xFF9E9E9E),
                    selected = filterStatus == "closed",
                    onClick = { filterStatus = if (filterStatus == "closed") null else "closed" }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Search + Filter ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search jobs…", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    trailingIcon = {
                        AnimatedVisibility(searchQuery.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, null, tint = Color.Gray)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedContainerColor = Color(0xFFEEF7E8),
                        unfocusedContainerColor = Color(0xFFEEF7E8)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box {
                    IconButton(
                        onClick = { showFilterMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (filterStatus != null) GreenPrimary else Color(0xFFEEF7E8))
                            .border(1.dp, GreenPrimary, CircleShape)
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter",
                            tint = if (filterStatus != null) Color.White else GreenPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        listOf(null to "All", "open" to "Open", "closed" to "Closed").forEach { (s, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { filterStatus = s; showFilterMenu = false },
                                leadingIcon = {
                                    if (filterStatus == s)
                                        Icon(Icons.Outlined.Check, null, tint = GreenPrimary)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Content ───────────────────────────────────────────────────────
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }

                filtered.isEmpty() -> EmptyJobsPlaceholder(
                    hasJobs = jobs.isNotEmpty(),
                    onCreateClick = { showCreateSheet = true }
                )

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered, key = { it.id }) { job ->
                        FarmerJobCard(
                            job = job,
                            onEdit   = { jobToEdit = job },
                            onDelete = { jobToDelete = job },
                            onToggleStatus = {
                                scope.launch {
                                    try {
                                        val newStatus = if (job.status == "open") "closed" else "open"
                                        SupabaseClientProvider.client
                                            .postgrest["jobs"]
                                            .update(mapOf("status" to newStatus)) {
                                                filter { eq("id", job.id) }
                                            }
                                        loadJobs()
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
    }

    // ── Create Sheet ──────────────────────────────────────────────────────────
    if (showCreateSheet) {
        JobFormSheet(
            title = "Post New Job",
            initial = null,
            onDismiss = { showCreateSheet = false },
            onConfirm = { title, description, location, salary ->
                scope.launch {
                    try {
                        SupabaseClientProvider.client
                            .postgrest["jobs"]
                            .insert(
                                CreateJob(
                                    title = title,
                                    description = description,
                                    farmer_id = farmerId,
                                    location = location.ifBlank { null },
                                    salary = salary
                                )
                            )
                        showCreateSheet = false
                        loadJobs()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        )
    }

    // ── Edit Sheet ────────────────────────────────────────────────────────────
    jobToEdit?.let { job ->
        JobFormSheet(
            title = "Edit Job",
            initial = job,
            onDismiss = { jobToEdit = null },
            onConfirm = { title, description, location, salary ->
                scope.launch {
                    try {
                        SupabaseClientProvider.client
                            .postgrest["jobs"]
                            .update(
                                mapOf(
                                    "title"       to title,
                                    "description" to description,
                                    "location"    to location.ifBlank { null },
                                    "salary"      to salary
                                )
                            ) {
                                filter { eq("id", job.id) }
                            }
                        jobToEdit = null
                        loadJobs()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        )
    }

    // ── Delete Dialog ─────────────────────────────────────────────────────────
    jobToDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            icon = {
                Icon(Icons.Outlined.DeleteForever, null, tint = RedError, modifier = Modifier.size(32.dp))
            },
            title = { Text("Delete Job?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${job.title}\" will be permanently deleted along with all its applications. This cannot be undone.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                SupabaseClientProvider.client
                                    .postgrest["jobs"]
                                    .delete { filter { eq("id", job.id) } }
                                jobs = jobs.filter { it.id != job.id }
                            } catch (e: Exception) { e.printStackTrace() }
                            finally { jobToDelete = null }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { jobToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── Job Card ─────────────────────────────────────────────────────────────────

@Composable
private fun FarmerJobCard(
    job: Job,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isOpen = job.status == "open"
    val statusColor = if (isOpen) GreenPrimary else Color(0xFF9E9E9E)

    val daysAgo = remember(job.created_at) {
        try {
            val created = Instant.parse(job.created_at ?: return@remember null)
            ChronoUnit.DAYS.between(created, Instant.now())
        } catch (e: Exception) { null }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp, if (isOpen) GreenPrimary.copy(alpha = 0.4f) else Color(0xFFDDDDDD)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Top row ───────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.Top) {
                // Icon badge
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isOpen) GreenPrimary.copy(alpha = 0.12f) else Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Agriculture,
                        null,
                        tint = if (isOpen) GreenPrimary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Title + status badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            job.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isOpen) "Open" else "Closed",
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Salary
                    job.salary?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Payments, null, tint = GreenPrimaryDark, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${it.toInt()} DZD/day",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = GreenPrimaryDark
                            )
                        }
                    }
                }

                // Actions menu
                JobActionsMenu(
                    isOpen = isOpen,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onToggleStatus = onToggleStatus
                )
            }

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                job.description,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(8.dp))

            // ── Bottom meta row ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Location
                job.location?.let {
                    Icon(Icons.Outlined.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(it, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(12.dp))
                }

                // Date
                daysAgo?.let {
                    Icon(Icons.Outlined.Schedule, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        if (it == 0L) "Today" else "$it days ago",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ─── Actions overflow menu ────────────────────────────────────────────────────

@Composable
private fun JobActionsMenu(
    isOpen: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = GreenPrimary) },
                onClick = { expanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(if (isOpen) "Close job" else "Reopen job") },
                leadingIcon = {
                    Icon(
                        if (isOpen) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        null,
                        tint = if (isOpen) Color(0xFFFFA000) else GreenPrimary
                    )
                },
                onClick = { expanded = false; onToggleStatus() }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Delete", color = RedError) },
                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = RedError) },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}

// ─── Job Form Sheet (Create / Edit) ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobFormSheet(
    title: String,
    initial: Job?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, location: String, salary: Double?) -> Unit
) {
    var jobTitle   by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var location   by remember { mutableStateOf(initial?.location ?: "") }
    var salaryText by remember { mutableStateOf(initial?.salary?.toInt()?.toString() ?: "") }
    var isSaving   by remember { mutableStateOf(false) }

    val titleError = jobTitle.isBlank()
    val descError  = description.isBlank()
    val salaryError = salaryText.isNotBlank() && salaryText.toDoubleOrNull() == null

    val isValid = !titleError && !descError && !salaryError

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (initial == null) Icons.Outlined.AddCircleOutline else Icons.Outlined.Edit,
                        null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Title field
            FormField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = "Job Title *",
                placeholder = "e.g. Olive Harvest Helper",
                icon = Icons.Outlined.WorkOutline,
                isError = jobTitle.isNotBlank() && titleError,
                errorMessage = "Title is required"
            )

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                placeholder = { Text("Describe the work, hours, requirements…") },
                leadingIcon = { Icon(Icons.Outlined.Description, null, tint = if (description.isBlank() && description.isNotEmpty()) RedError else GreenPrimary) },
                shape = RoundedCornerShape(14.dp),
                minLines = 3,
                maxLines = 5,
                colors = outlinedFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Location field
            FormField(
                value = location,
                onValueChange = { location = it },
                label = "Location",
                placeholder = "e.g. Ouargla, Algeria",
                icon = Icons.Outlined.LocationOn
            )

            // Salary field
            FormField(
                value = salaryText,
                onValueChange = { salaryText = it },
                label = "Daily Wage (DZD)",
                placeholder = "e.g. 1500",
                icon = Icons.Outlined.Payments,
                keyboardType = KeyboardType.Number,
                isError = salaryError,
                errorMessage = "Enter a valid number"
            )

            Spacer(Modifier.height(4.dp))

            // Confirm button
            Button(
                onClick = {
                    if (!isValid) return@Button
                    isSaving = true
                    onConfirm(
                        jobTitle.trim(),
                        description.trim(),
                        location.trim(),
                        salaryText.toDoubleOrNull()
                    )
                },
                enabled = isValid && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        if (initial == null) Icons.Outlined.Add else Icons.Outlined.Check,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (initial == null) "Post Job" else "Save Changes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
            ) {
                Text("Cancel", color = Color.Gray)
            }
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color.LightGray) },
        leadingIcon = { Icon(icon, null, tint = if (isError) RedError else GreenPrimary) },
        isError = isError,
        supportingText = if (isError) ({ Text(errorMessage, color = RedError, fontSize = 11.sp) }) else null,
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = outlinedFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    unfocusedBorderColor = Color(0xFFDDDDDD),
    focusedLabelColor = GreenPrimary,
    cursorColor = GreenPrimary
)

@Composable
private fun StatChip(
    label: String,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) color.copy(alpha = 0.15f) else Color(0xFFEEEEEE),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, color) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) color else Color.Gray
            )
            Surface(shape = CircleShape, color = color.copy(alpha = if (selected) 0.2f else 0.1f)) {
                Text(
                    count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyJobsPlaceholder(hasJobs: Boolean, onCreateClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.WorkOff,
                    null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                if (hasJobs) "No jobs match your filter" else "No job posts yet",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.DarkGray
            )
            Text(
                if (hasJobs) "Try clearing the search or filter." else "Tap the button below to post your first job.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!hasJobs) {
                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Post a Job")
                }
            }
        }
    }
}