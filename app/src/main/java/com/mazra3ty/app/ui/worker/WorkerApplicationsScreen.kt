package com.mazra3ty.app.ui.worker

import androidx.compose.foundation.background
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
import com.mazra3ty.app.database.types.Application
import com.mazra3ty.app.database.types.Job
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerApplicationsScreen(
    workerId: String,
    onBack: () -> Unit
) {
    var applications by remember { mutableStateOf<List<Pair<Application, Job?>>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }
    var error        by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val apps = SupabaseClientProvider.client
                .postgrest["applications"]
                .select { filter { eq("worker_id", workerId) } }
                .decodeList<Application>()

            val jobIds = apps.map { it.job_id }.distinct()
            val jobs   = if (jobIds.isNotEmpty()) {
                SupabaseClientProvider.client
                    .postgrest["jobs"]
                    .select { filter { isIn("id", jobIds) } }
                    .decodeList<Job>()
            } else emptyList()

            val jobMap   = jobs.associateBy { it.id }
            applications = apps.map { it to jobMap[it.job_id] }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAF5),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Applications", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${applications.size} sent", fontSize = 12.sp, color = Color(0xFF888888))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ArrowBack, null, tint = GreenPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = GreenPrimary) }

            applications.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No applications yet", fontWeight = FontWeight.SemiBold)
                    Text("Apply to jobs to see them here", fontSize = 13.sp, color = Color(0xFF888888))
                }
            }

            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(applications) { (app, job) ->
                    WorkerApplicationCard(app = app, job = job)
                }
            }
        }
    }
}

@Composable
private fun WorkerApplicationCard(app: Application, job: Job?) {
    val (statusColor, statusBg, statusLabel, statusIcon) = when (app.status) {
        "accepted" -> listOf(Color(0xFF2E7D32), Color(0xFFE8F5E9), "Accepted", "✅")
        "rejected" -> listOf(Color(0xFFC62828), Color(0xFFFFEBEE), "Rejected", "❌")
        else       -> listOf(Color(0xFFE65100), Color(0xFFFFF3E0), "Pending",  "⏳")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        job?.title ?: "Unknown Job",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = Color(0xFF1A1A1A)
                    )
                    job?.location?.let {
                        Text("📍 $it", fontSize = 12.sp, color = Color(0xFF888888))
                    }
                    job?.salary?.let {
                        Text("💰 ${it.toInt()} DA/day", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.Medium)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg as Color
                ) {
                    Text(
                        "$statusIcon $statusLabel",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize   = 12.sp,
                        color      = statusColor as Color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            app.created_at?.take(10)?.let {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(8.dp))
                Text("Applied on: $it", fontSize = 11.sp, color = Color(0xFFAAAAAA))
            }
        }
    }
}