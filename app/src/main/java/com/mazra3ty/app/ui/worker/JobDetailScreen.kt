package com.mazra3ty.app.ui.worker

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mazra3ty.app.ui.theme.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// MAIN: Job Detail Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailSheet(
    job: Job,
    workerEmail: String,
    workerName: String,
    onDismiss: () -> Unit,
    onApplied: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isApplying        by remember { mutableStateOf(false) }
    var isApplied         by remember { mutableStateOf(false) }
    val scope             = rememberCoroutineScope()

    // Bottom Sheet
    ModalBottomSheet(
        onDismissRequest   = onDismiss,
        sheetState         = sheetState,
        containerColor     = Color.White,
        shape              = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle         = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFE0E0E0), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {

            // ── Header ──
            JobDetailHeader(job)

            // ── Info Chips ──
            JobInfoChips(job)

            // ── Description ──
            if (job.description.isNotBlank()) {
                DetailSection(
                    icon  = Icons.Outlined.Description,
                    title = "Job Description"
                ) {
                    Text(
                        text  = job.description,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }

            // ── Requirements ──
            if (job.requirements.isNotBlank()) {
                DetailSection(
                    icon  = Icons.Outlined.Checklist,
                    title = "Requirements"
                ) {
                    Text(
                        text  = job.requirements,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }

            // ── Farmer Info ──
            DetailSection(
                icon  = Icons.Outlined.Person,
                title = "Posted By"
            ) {
                FarmerInfoCard(job)
            }

            Spacer(Modifier.height(24.dp))

            // ── Apply Button ──
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (isApplied) {
                    // تم التقديم
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        color    = GreenLight
                    ) {
                        Row(
                            modifier              = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint     = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Application Sent Successfully!",
                                color      = GreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick  = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        )
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                strokeWidth = 2.dp,
                                modifier    = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Send,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Send My Info to Farmer",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Confirm Dialog ──
    if (showConfirmDialog) {
        ApplyConfirmDialog(
            job         = job,
            workerName  = workerName,
            workerEmail = workerEmail,
            onConfirm   = {
                showConfirmDialog = false
                isApplying        = true
                scope.launch {
                    // TODO: هنا تضيف كود Supabase لإرسال الطلب
                    kotlinx.coroutines.delay(1500)
                    isApplying = false
                    isApplied  = true
                    onApplied()
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun JobDetailHeader(job: Job) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(GreenLight, Color.White)
                )
            )
            .padding(20.dp)
    ) {
        Column {
            // Category Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GreenPrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    text     = job.category,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color    = GreenPrimary,
                    style    = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                text       = job.title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            // Price
            Text(
                text       = job.price,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = GreenPrimary,
                fontSize   = 22.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info Chips
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun JobInfoChips(job: Job) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InfoChip(
            icon  = Icons.Outlined.Schedule,
            text  = job.duration,
            modifier = Modifier.weight(1f)
        )
        InfoChip(
            icon  = Icons.Outlined.LocationOn,
            text  = job.location,
            modifier = Modifier.weight(2f)
        )
    }

    if (job.postedDate.isNotBlank()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                null,
                tint     = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Posted: ${job.postedDate}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        color    = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint     = GreenPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text  = text,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DetailSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary
            )
        }
        Spacer(Modifier.height(8.dp))
        content()
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color    = Color(0xFFF0F0F0)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Farmer Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FarmerInfoCard(job: Job) {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = GreenLight,
        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .background(GreenPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = job.farmerName.firstOrNull()?.toString() ?: "F",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text       = job.farmerName.ifBlank { "Farm Owner" },
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
                if (job.farmerEmail.isNotBlank()) {
                    Text(
                        text  = job.farmerEmail,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Confirm Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ApplyConfirmDialog(
    job: Job,
    workerName: String,
    workerEmail: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape    = RoundedCornerShape(20.dp),
            color    = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Icon
                Box(
                    modifier         = Modifier
                        .size(56.dp)
                        .background(GreenLight, CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Send,
                        null,
                        tint     = GreenPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text       = "Send Application?",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text  = "Your info will be sent to the farmer for this job:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(16.dp))

                // معلومات ستُرسل
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8F8F8)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "📋 Job: ${job.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "👤 Your Name: $workerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "📧 Your Email: $workerEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        )
                    ) {
                        Text("Send", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}