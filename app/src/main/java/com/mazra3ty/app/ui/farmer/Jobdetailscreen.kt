package com.mazra3ty.app.ui.farmer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.mazra3ty.app.database.types.Application
import com.mazra3ty.app.database.types.CreateApplication
import com.mazra3ty.app.database.types.Job
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    jobId: String,
    currentUserId: String,          // the logged-in worker's id
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var job      by remember { mutableStateOf<Job?>(null) }
    var farmer   by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Apply state: null = not applied, "pending" / "accepted" / "rejected"
    var applyStatus   by remember { mutableStateOf<String?>(null) }
    var isApplying    by remember { mutableStateOf(false) }
    var applyError    by remember { mutableStateOf<String?>(null) }
    var successDialog by remember { mutableStateOf(false) }

    // ── Load job + farmer + existing application ──────────────────────────────
    LaunchedEffect(jobId) {
        scope.launch {
            try {
                val loadedJob = SupabaseClientProvider.client
                    .postgrest["jobs"]
                    .select { filter { eq("id", jobId) } }
                    .decodeSingle<Job>()
                job = loadedJob

                // Fetch farmer info
                val loadedFarmer = SupabaseClientProvider.client
                    .postgrest["users"]
                    .select { filter { eq("id", loadedJob.farmer_id) } }
                    .decodeSingle<User>()
                farmer = loadedFarmer

                // Check if already applied
                val existing = SupabaseClientProvider.client
                    .postgrest["applications"]
                    .select {
                        filter {
                            eq("job_id", jobId)
                            eq("worker_id", currentUserId)
                        }
                    }
                    .decodeList<Application>()
                if (existing.isNotEmpty()) applyStatus = existing.first().status

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // ── Apply action ─────────────────────────────────────────────────────────
    fun applyForJob() {
        scope.launch {
            isApplying = true
            applyError = null
            try {
                SupabaseClientProvider.client
                    .postgrest["applications"]
                    .insert(
                        mapOf(
                            "job_id"    to jobId,
                            "worker_id" to currentUserId,
                            "status"    to "pending"
                        )
                    )
                applyStatus = "pending"
                successDialog = true
            } catch (e: Exception) {
                applyError = "Failed to apply. Please try again."
                e.printStackTrace()
            } finally {
                isApplying = false
            }
        }
    }

    // ── Scaffold ─────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7F2)
    ) { padding ->

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }

            job == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Job not found", color = Color.Gray)
            }

            else -> {
                val j = job!!
                val f = farmer

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Hero card ─────────────────────────────────────────────
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Agriculture,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = j.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                j.location?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.LocationOn, null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                                    }
                                }
                            }

                            // Status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = j.status.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // ── Salary + date ─────────────────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        j.salary?.let { salary ->
                            InfoChip(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.Payments,
                                label = "Salary",
                                value = "${salary.toInt()} DZD/J",
                                iconTint = GreenPrimaryDark,
                                bgColor = Color(0xFFE8F5E9)
                            )
                        }
                        j.created_at?.let { createdAt ->
                            val formatted = remember(createdAt) {
                                try {
                                    val instant = Instant.parse(createdAt)
                                    DateTimeFormatter
                                        .ofPattern("dd MMM yyyy")
                                        .withZone(ZoneId.systemDefault())
                                        .format(instant)
                                } catch (e: Exception) { createdAt }
                            }
                            InfoChip(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.CalendarToday,
                                label = "Posted",
                                value = formatted,
                                iconTint = Color(0xFF1E88E5),
                                bgColor = Color(0xFFE3F2FD)
                            )
                        }
                    }

                    // ── Description ───────────────────────────────────────────
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, GreenPrimary.copy(alpha = 0.18f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Description",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = j.description,
                                fontSize = 14.sp,
                                color = GrayDark,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    // ── Farmer info ───────────────────────────────────────────
                    if (f != null) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, GreenPrimary.copy(alpha = 0.18f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(GreenPrimary.copy(alpha = 0.12f))
                                        .border(2.dp, GreenPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Person, null,
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        f.full_name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Farmer",
                                        fontSize = 12.sp,
                                        color = GreenPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    f.phone?.let {
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.Phone, null,
                                                tint = GrayMedium,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(it, fontSize = 12.sp, color = GrayMedium)
                                        }
                                    }
                                }
                                Icon(Icons.Outlined.ChevronRight, null, tint = GrayMedium)
                            }
                        }
                    }

                    // ── Error ─────────────────────────────────────────────────
                    applyError?.let {
                        Text(it, color = RedError, fontSize = 13.sp)
                    }

                    // ── Apply button ──────────────────────────────────────────
                    Spacer(Modifier.height(4.dp))

                    Spacer(Modifier.height(4.dp))

                    when {
                        j.status != "open" -> {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Job is Closed", fontSize = 15.sp)
                            }
                        }

                        applyStatus != null -> {

                            val (bg, text, icon) = when (applyStatus) {
                                "accepted" -> Triple(
                                    Color(0xFF2E7D32),
                                    "Application Accepted",
                                    Icons.Outlined.Done
                                )
                                "rejected" -> Triple(
                                    RedError,
                                    "Application Rejected",
                                    Icons.Outlined.Dangerous
                                )
                                else -> Triple(
                                    GrayMedium,
                                    "Application Pending",
                                    Icons.Outlined.Timer
                                )
                            }

                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = bg.copy(alpha = 0.15f),
                                    disabledContentColor = bg
                                )
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        else -> {
                            Button(
                                onClick = { applyForJob() },
                                enabled = !isApplying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                if (isApplying) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Outlined.Send, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Apply Now",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // ── Success dialog ────────────────────────────────────────────────────────
    if (successDialog) {
        AlertDialog(
            onDismissRequest = { successDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Application Sent!", fontWeight = FontWeight.Bold) },
            text = { Text("Your application has been submitted. The farmer will review it soon.") },
            confirmButton = {
                Button(
                    onClick = { successDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Great!", color = Color.White) }
            }
        )
    }
}

// ─── Info Chip ────────────────────────────────────────────────────────────────

@Composable
private fun InfoChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    bgColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconTint.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, fontSize = 11.sp, color = GrayMedium)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}