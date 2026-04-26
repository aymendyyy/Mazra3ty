package com.mazra3ty.app.ui.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.Application
import com.mazra3ty.app.database.types.Job
import com.mazra3ty.app.database.types.UserWithProfile
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─── Status palette ───────────────────────────────────────────────────────────

private data class StatusStyle(
    val color:   Color,
    val bgColor: Color,
    val label:   String,
    val icon:    ImageVector
)

private fun statusStyle(s: String) = when (s) {
    "accepted" -> StatusStyle(Color(0xFF2E7D32), Color(0xFFE8F5E9), "Accepted", Icons.Outlined.CheckCircle)
    "rejected" -> StatusStyle(Color(0xFFC62828), Color(0xFFFFEBEE), "Rejected", Icons.Outlined.Cancel)
    else       -> StatusStyle(Color(0xFFE65100), Color(0xFFFFF3E0), "Pending",  Icons.Outlined.HourglassTop)
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsMonitorScreen(onBack: () -> Unit) {

    val scope = rememberCoroutineScope()

    var applications by remember { mutableStateOf<List<Application>>(emptyList()) }
    // FIX: Changed from Map<String, User> to Map<String, UserWithProfile>
    // This fixes the crash caused by non-nullable email field in User
    var usersMap     by remember { mutableStateOf<Map<String, UserWithProfile>>(emptyMap()) }
    var jobsMap      by remember { mutableStateOf<Map<String, Job>>(emptyMap()) }
    var isLoading    by remember { mutableStateOf(true) }
    var searchQuery  by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                applications = SupabaseClientProvider.client
                    .postgrest["applications"].select().decodeList<Application>()
                    .sortedByDescending { it.created_at }

                // FIX: Use user_with_profile view instead of users table.
                // UserWithProfile has email as String? (nullable) which prevents
                // JSON deserialization crashes when email is missing.
                usersMap = SupabaseClientProvider.client
                    .postgrest["user_with_profile"].select().decodeList<UserWithProfile>()
                    .associateBy { it.id }

                jobsMap = SupabaseClientProvider.client
                    .postgrest["jobs"].select().decodeList<Job>()
                    .associateBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally { isLoading = false }
        }
    }

    val filtered = applications.filter { app ->
        val worker = usersMap[app.worker_id]
        val job    = jobsMap[app.job_id]
        val farmer = job?.farmer_id?.let { usersMap[it] }
        val q = searchQuery.trim()
        val matchSearch = q.isEmpty() ||
                worker?.full_name?.contains(q, true) == true ||
                farmer?.full_name?.contains(q, true) == true ||
                job?.title?.contains(q, true) == true ||
                worker?.role?.contains(q, true) == true   // also search by role
        val matchStatus = statusFilter == null || app.status == statusFilter
        matchSearch && matchStatus
    }

    val total    = applications.size
    val pending  = applications.count { it.status == "pending" }
    val accepted = applications.count { it.status == "accepted" }
    val rejected = applications.count { it.status == "rejected" }

    Scaffold(containerColor = Color(0xFFF6F7FA)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Dark header with stats ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B3A2B), Color(0xFF2D5A3D))))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Text("Applications Overview", color = Color.White.copy(0.65f), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("$total Total", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniPill("$pending Pending",  Color(0xFFFF8A65), Icons.Outlined.HourglassTop)
                        MiniPill("$accepted Accepted", Color(0xFF81C784), Icons.Outlined.CheckCircle)
                        MiniPill("$rejected Rejected", Color(0xFFE57373), Icons.Outlined.Cancel)
                    }
                }
            }

            // ── Search + chips ────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Worker, farmer, role, or job title…", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = GreenPrimary, modifier = Modifier.size(20.dp)) },
                    trailingIcon  = {
                        AnimatedVisibility(searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape  = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = GreenPrimary,
                        unfocusedBorderColor    = Color(0xFFE0E0E0),
                        focusedContainerColor   = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth().height(52.dp)
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf(
                        null       to ("All"      to GreenPrimary),
                        "pending"  to ("Pending"  to Color(0xFFE65100)),
                        "accepted" to ("Accepted" to Color(0xFF2E7D32)),
                        "rejected" to ("Rejected" to Color(0xFFC62828))
                    )
                    items(chips) { (status, pair) ->
                        val (label, chipColor) = pair
                        val selected = statusFilter == status
                        FilterChip(
                            selected = selected,
                            onClick  = { statusFilter = status },
                            label    = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor,
                                selectedLabelColor     = Color.White,
                                containerColor         = Color.White,
                                labelColor             = Color(0xFF555555)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = selected,
                                selectedBorderColor = chipColor,
                                borderColor         = Color(0xFFDDDDDD)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                AnimatedVisibility(!isLoading, enter = fadeIn(tween(300))) {
                    Text(
                        "${filtered.size} result${if (filtered.size != 1) "s" else ""}",
                        fontSize = 11.sp, color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ── List ──────────────────────────────────────────────────────
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GreenPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading applications…", color = Color.Gray, fontSize = 13.sp)
                    }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.SearchOff, null, tint = Color(0xFFDDDDDD), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No applications found", color = Color(0xFFAAAAAA), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        if (searchQuery.isNotEmpty() || statusFilter != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { searchQuery = ""; statusFilter = null }) {
                                Text("Clear filters", color = GreenPrimary)
                            }
                        }
                    }
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filtered, key = { it.id }) { app ->
                        AppDetailCard(
                            application = app,
                            worker      = usersMap[app.worker_id],
                            job         = jobsMap[app.job_id],
                            farmer      = jobsMap[app.job_id]?.farmer_id?.let { usersMap[it] }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ─── Application detail card ──────────────────────────────────────────────────

@Composable
private fun AppDetailCard(
    application: Application,
    worker:      UserWithProfile?,   // FIX: was User?
    job:         Job?,
    farmer:      UserWithProfile?    // FIX: was User?
) {
    val style   = statusStyle(application.status)
    val dateStr = remember(application.created_at) {
        runCatching {
            val instant = Instant.parse(application.created_at ?: return@runCatching null)
            val days    = ChronoUnit.DAYS.between(instant, Instant.now())
            val fmt     = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault())
            when (days) {
                0L   -> "Today · ${fmt.format(instant)}"
                1L   -> "Yesterday · ${fmt.format(instant)}"
                else -> "${days}d ago · ${fmt.format(instant)}"
            }
        }.getOrNull()
    }

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column {

            // ── Status band ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(style.bgColor, style.bgColor.copy(0.4f))),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(style.icon, null, tint = style.color, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(style.label, color = style.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                dateStr?.let { Text(it, color = style.color.copy(0.7f), fontSize = 11.sp) }
            }

            Column(modifier = Modifier.padding(16.dp)) {

                // ── Participants ───────────────────────────────────────────
                SectionLabel("Participants")
                Spacer(Modifier.height(10.dp))

                // FIX: Use dynamic role label from actual user data
                PersonCard(
                    badge      = "APPLICANT · ${worker?.role?.uppercase() ?: "WORKER"}",
                    badgeColor = Color(0xFF1565C0),
                    user       = worker,
                    icon       = Icons.Outlined.Agriculture,
                    gradient   = listOf(Color(0xFF1565C0).copy(0.07f), Color(0xFF1976D2).copy(0.03f))
                )

                // Connector arrow
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(width = 1.5.dp, height = 10.dp).background(Color(0xFFDDDDDD)))
                        Box(
                            modifier         = Modifier.size(26.dp).clip(CircleShape)
                                .background(Color(0xFFF0F0F0))
                                .border(1.dp, Color(0xFFDDDDDD), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ArrowDownward, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(14.dp))
                        }
                        Box(Modifier.size(width = 1.5.dp, height = 10.dp).background(Color(0xFFDDDDDD)))
                    }
                }

                // FIX: Use dynamic role label from actual user data
                PersonCard(
                    badge      = "RECIPIENT · ${farmer?.role?.uppercase() ?: "FARMER"}",
                    badgeColor = Color(0xFF2E7D32),
                    user       = farmer,
                    icon       = Icons.Outlined.Yard,
                    gradient   = listOf(Color(0xFF2E7D32).copy(0.07f), Color(0xFF388E3C).copy(0.03f))
                )

                // ── Job details ────────────────────────────────────────────
                job?.let { j ->
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Job Details")
                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape    = RoundedCornerShape(14.dp),
                        color    = Color(0xFFFAFAFA),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    j.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 15.sp,
                                    color      = Color(0xFF1A1A1A),
                                    modifier   = Modifier.weight(1f),
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (j.status == "open") GreenPrimary.copy(0.12f) else Color(0xFFEEEEEE)
                                ) {
                                    Text(
                                        j.status.replaceFirstChar { it.uppercase() },
                                        color      = if (j.status == "open") GreenPrimaryDark else Color.Gray,
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier   = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                j.location?.let { JobChip(Icons.Outlined.LocationOn, it, GrayDark) }
                                j.salary?.let   { JobChip(Icons.Outlined.AttachMoney, "${it.toInt()} DZD/J", GreenPrimaryDark) }
                            }
                            if (j.description.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    j.description,
                                    fontSize = 12.sp, color = Color(0xFF666666),
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // ── Footer ────────────────────────────────────────────────
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "ID: ${application.id.take(8)}…",
                        fontSize = 10.sp, color = Color(0xFFCCCCCC), fontWeight = FontWeight.Medium
                    )
                    Surface(shape = RoundedCornerShape(20.dp), color = style.bgColor) {
                        Text(
                            style.label,
                            fontSize   = 10.sp, color = style.color,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Person card ──────────────────────────────────────────────────────────────

@Composable
private fun PersonCard(
    badge:      String,
    badgeColor: Color,
    user:       UserWithProfile?,   // FIX: was User?
    icon:       ImageVector,
    gradient:   List<Color>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(gradient), RoundedCornerShape(14.dp))
            .border(1.dp, badgeColor.copy(0.15f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(48.dp).clip(CircleShape)
                .background(badgeColor.copy(0.12f)).border(1.5.dp, badgeColor.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (user != null)
                Text(user.full_name.take(1).uppercase(), color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            else
                Icon(icon, null, tint = badgeColor.copy(0.5f), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Badge label (now dynamic with actual role)
            Text(badge, fontSize = 9.sp, color = badgeColor.copy(0.8f), fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                user?.full_name ?: "Unknown",
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1A1A1A),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            // FIX: phone is String? in UserWithProfile, no crash
            val contact = user?.phone ?: user?.email
            contact?.let {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (user?.phone != null) Icons.Outlined.Phone else Icons.Outlined.Email,
                        null, tint = Color.Gray, modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(it, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        // Show actual role badge
        Surface(shape = RoundedCornerShape(20.dp), color = badgeColor.copy(0.1f)) {
            Text(
                user?.role?.replaceFirstChar { it.uppercase() } ?: "—",
                color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
}

@Composable
private fun JobChip(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MiniPill(text: String, color: Color, icon: ImageVector) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = color.copy(0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(0.35f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}