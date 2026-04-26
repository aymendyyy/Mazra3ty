package com.mazra3ty.app.ui.worker

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.types.CreateWorkerPost
import com.mazra3ty.app.database.types.WorkerPostStatus
import com.mazra3ty.app.ui.theme.*
import com.mazra3ty.app.database.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

// ─── قائمة المهارات المتاحة ────────────────────────────────────────────────────

private val availableSkills = listOf(
    "🌾 Harvesting",
    "🌱 Planting",
    "🚜 Plowing",
    "💧 Irrigation",
    "🌿 Pruning",
    "🐄 Livestock Care",
    "📦 Packing & Sorting",
    "🔧 Equipment Repair",
    "🌳 Tree Climbing",
    "🧪 Pesticide Application",
    "🏗️ Farm Construction",
    "🚗 Transportation"
)

// ─── الشاشة الرئيسية ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkerPostScreen(
    workerId: String,
    workerName: String = "",
    onPostCreated: () -> Unit,
    onBack: () -> Unit
) {
    // ── حالة النموذج ──
    var title          by remember { mutableStateOf("") }
    var description    by remember { mutableStateOf("") }
    var location       by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var availability   by remember { mutableStateOf("") }
    var expectedSalary by remember { mutableStateOf("") }

    // ── حالة الواجهة ──
    var isLoading         by remember { mutableStateOf(false) }
    var showSuccess       by remember { mutableStateOf(false) }
    var errorMessage      by remember { mutableStateOf<String?>(null) }
    var currentStep       by remember { mutableStateOf(0) }
    var showSuccessDialog by remember { mutableStateOf(false) }  // ← جديد
    var canPost           by remember { mutableStateOf(true) }   // ← جديد
    var checkingLimit     by remember { mutableStateOf(true) }   // ← جديدval scope
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        try {
            val today = java.time.LocalDate.now().toString()
            val todayPosts = SupabaseClientProvider.client
                .postgrest["worker_posts"]
                .select { filter { eq("worker_id", workerId) } }
                .decodeList<com.mazra3ty.app.database.types.WorkerPost>()
                .count { post -> post.created_at?.startsWith(today) == true }
            canPost = todayPosts == 0
        } catch (e: Exception) {
            canPost = true
        } finally {
            checkingLimit = false
        }
    }

    // ── Validation ──
    val isTitleValid    = title.trim().length >= 3
    val isDescValid     = description.trim().length >= 10
    val isLocationValid = location.trim().isNotEmpty()
    val hasSkills       = selectedSkills.isNotEmpty()

    val step0Valid = isTitleValid && isDescValid
    val step1Valid = hasSkills
    val canSubmit  = step0Valid && step1Valid && isLocationValid && !isLoading

    Scaffold(
        containerColor = Color(0xFFF5F9F0),
        topBar = {
            WorkerPostTopBar(
                currentStep = currentStep,
                onBack      = onBack
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── شريط التقدم ──
            StepProgressBar(currentStep = currentStep)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── بانر الترحيب ──
                WorkerPostBanner(workerName = workerName)

                // ═══ STEP 0: معلومات المنشور ═══════════════════════════════════
                AnimatedVisibility(
                    visible = currentStep == 0,
                    enter   = fadeIn(tween(300)) + slideInHorizontally { it },
                    exit    = fadeOut(tween(200)) + slideOutHorizontally { -it }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        StepHeader(
                            step  = 1,
                            title = "Tell Farmers About Yourself",
                            subtitle = "What service do you offer?"
                        )

                        SectionCard(
                            icon  = Icons.Outlined.Person,
                            title = "Basic Info"
                        ) {
                            // عنوان المنشور
                            FormField(
                                label         = "Post Title *",
                                value         = title,
                                onValueChange = { title = it },
                                placeholder   = "e.g. Experienced Tomato Picker Available",
                                icon          = Icons.Outlined.Title,
                                isError       = title.isNotEmpty() && !isTitleValid,
                                errorText     = "At least 3 characters required"
                            )

                            // وصف الخدمة
                            OutlinedTextField(
                                value         = description,
                                onValueChange = { description = it },
                                label         = { Text("Describe Your Service *", fontSize = 13.sp) },
                                placeholder   = {
                                    Text(
                                        "e.g. I have 5 years of experience in tomato and pepper harvesting. I work quickly and carefully. Available for short and long-term contracts.",
                                        color    = Color(0xFFAAAAAA),
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Description, null,
                                        tint     = GreenPrimary,
                                        modifier = Modifier
                                            .padding(top = 14.dp)
                                            .size(20.dp)
                                    )
                                },
                                isError        = description.isNotEmpty() && !isDescValid,
                                supportingText = {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (description.isNotEmpty() && !isDescValid) {
                                            Text(
                                                "At least 10 characters",
                                                color    = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp
                                            )
                                        } else {
                                            Spacer(Modifier.width(1.dp))
                                        }
                                        Text(
                                            "${description.length}",
                                            fontSize = 11.sp,
                                            color    = Color(0xFFAAAAAA)
                                        )
                                    }
                                },
                                minLines = 4,
                                maxLines = 8,
                                shape    = RoundedCornerShape(14.dp),
                                colors   = workerFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // التوفر
                            FormField(
                                label         = "Availability",
                                value         = availability,
                                onValueChange = { availability = it },
                                placeholder   = "e.g. Weekdays, Full-time, Immediately",
                                icon          = Icons.Outlined.CalendarMonth
                            )
                        }
                    }
                }

                // ═══ STEP 1: المهارات ══════════════════════════════════════════
                AnimatedVisibility(
                    visible = currentStep == 1,
                    enter   = fadeIn(tween(300)) + slideInHorizontally { it },
                    exit    = fadeOut(tween(200)) + slideOutHorizontally { -it }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        StepHeader(
                            step     = 2,
                            title    = "Select Your Skills",
                            subtitle = "Choose all that apply (${selectedSkills.size} selected)"
                        )

                        SectionCard(
                            icon  = Icons.Outlined.Star,
                            title = "Skills & Expertise"
                        ) {
                            SkillsGrid(
                                skills         = availableSkills,
                                selectedSkills = selectedSkills,
                                onToggle       = { skill ->
                                    selectedSkills = if (skill in selectedSkills) {
                                        selectedSkills - skill
                                    } else {
                                        selectedSkills + skill
                                    }
                                }
                            )

                            if (selectedSkills.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "✅ ${selectedSkills.size} skill(s) selected",
                                    fontSize   = 12.sp,
                                    color      = GreenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    "⚠️ Please select at least one skill",
                                    fontSize = 12.sp,
                                    color    = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }

                // ═══ STEP 2: الموقع والسعر ════════════════════════════════════
                AnimatedVisibility(
                    visible = currentStep == 2,
                    enter   = fadeIn(tween(300)) + slideInHorizontally { it },
                    exit    = fadeOut(tween(200)) + slideOutHorizontally { -it }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        StepHeader(
                            step     = 3,
                            title    = "Location & Expected Pay",
                            subtitle = "Help farmers find you"
                        )

                        SectionCard(
                            icon  = Icons.Outlined.LocationOn,
                            title = "Your Location"
                        ) {
                            FormField(
                                label         = "City / Region *",
                                value         = location,
                                onValueChange = { location = it },
                                placeholder   = "e.g. Ghardaia, Biskra, Ouargla",
                                icon          = Icons.Outlined.LocationOn,
                                isError       = location.isNotEmpty() && !isLocationValid,
                                errorText     = "Location is required"
                            )
                        }

                        SectionCard(
                            icon  = Icons.Outlined.Payments,
                            title = "Expected Salary (Optional)"
                        ) {
                            OutlinedTextField(
                                value         = expectedSalary,
                                onValueChange = {
                                    expectedSalary = it.filter { c -> c.isDigit() }
                                },
                                label         = { Text("Daily Rate (DA)", fontSize = 13.sp) },
                                placeholder   = { Text("e.g. 1500", color = Color(0xFFAAAAAA)) },
                                leadingIcon   = {
                                    Icon(Icons.Outlined.Payments, null, tint = GreenPrimary)
                                },
                                trailingIcon  = {
                                    if (expectedSalary.isNotEmpty()) {
                                        Text(
                                            "DA/day",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = GreenPrimary,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    }
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true,
                                shape      = RoundedCornerShape(14.dp),
                                colors     = workerFieldColors(),
                                modifier   = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(4.dp))
                            Text(
                                "💡 Leaving this empty means you're open to negotiation",
                                fontSize = 11.sp,
                                color    = Color(0xFF888888)
                            )
                        }

                        // ── ملخص المنشور قبل النشر ──
                        PostPreviewCard(
                            title          = title,
                            description    = description,
                            skills         = selectedSkills,
                            location       = location,
                            availability   = availability,
                            expectedSalary = expectedSalary,
                            workerName     = workerName
                        )

                        // ── رسالة الحد اليومي ──
                        if (!canPost && !checkingLimit) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF3E0)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⚠️", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            "Daily limit reached",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize   = 13.sp,
                                            color      = Color(0xFFE65100)
                                        )
                                        Text(
                                            "You can only post once per day. Come back tomorrow!",
                                            fontSize = 12.sp,
                                            color    = Color(0xFF888888)
                                        )
                                    }
                                }
                            }
                        }

                        // ── رسالة خطأ ──
                        AnimatedVisibility(visible = errorMessage != null) {
                            errorMessage?.let { msg ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier          = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.ErrorOutline,
                                            null,
                                            tint     = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            msg,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // ── أزرار التنقل بين الخطوات ──
            StepNavigationButtons(
                currentStep  = currentStep,
                totalSteps   = 3,
                canGoNext = when (currentStep) {
                    0    -> step0Valid
                    1    -> step1Valid
                    else -> canSubmit && canPost
                },
                isLoading    = isLoading,
                showSuccess  = showSuccess,
                onBack       = {
                    if (currentStep > 0) currentStep-- else onBack()
                },
                onNext       = {
                    if (currentStep < 2) {
                        currentStep++
                    }
                },
                onPublish = {
                    isLoading    = true
                    errorMessage = null
                    val post = CreateWorkerPost(
                        worker_id   = workerId,
                        title       = title.trim(),
                        description = buildString {
                            append(description.trim())
                            if (availability.isNotBlank()) append("\n\n⏰ Availability: ${availability.trim()}")
                            if (expectedSalary.isNotBlank()) append("\n💰 Expected: ${expectedSalary} DA/day")
                        },
                        skills   = selectedSkills,
                        location = location.trim().ifEmpty { null },
                        status   = WorkerPostStatus.active
                    )
                    scope.launch {
                        try {
                            SupabaseClientProvider.client
                                .postgrest["worker_posts"]
                                .insert(post)
                            isLoading         = false
                            showSuccessDialog = true
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to publish"
                            isLoading    = false
                        }
                    }
                }
            )
        }
    }
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        title          = ""
                        description    = ""
                        location       = ""
                        selectedSkills = emptyList()
                        availability   = ""
                        expectedSalary = ""
                        currentStep    = 0
                        canPost        = false
                        onPostCreated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Great!") }
            },
            icon  = { Text("🎉", fontSize = 40.sp) },
            title = {
                Text(
                    "Post Published!",
                    fontWeight = FontWeight.Bold,
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Text(
                    "Your post is now live. Farmers can see it and contact you.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color     = Color(0xFF555555)
                )
            },
            containerColor = Color.White,
            shape          = RoundedCornerShape(20.dp)
        )
    }
}

// ─── Top Bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerPostTopBar(
    currentStep: Int,
    onBack: () -> Unit
) {
    val stepTitles = listOf("My Info", "Skills", "Location & Pay")
    TopAppBar(
        title = {
            Column {
                Text(
                    "Create Worker Post",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = Color(0xFF1A1A1A)
                )
                Text(
                    "Step ${currentStep + 1} of 3 — ${stepTitles[currentStep]}",
                    fontSize = 12.sp,
                    color    = Color(0xFF888888)
                )
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
                    Icon(
                        Icons.Outlined.ArrowBack, null,
                        tint     = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

// ─── شريط تقدم الخطوات ─────────────────────────────────────────────────────────

@Composable
private fun StepProgressBar(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val isCompleted = index < currentStep
            val isCurrent   = index == currentStep

            // خط بين الدوائر
            if (index > 0) {
                HorizontalDivider(
                    modifier  = Modifier.weight(1f),
                    color     = if (isCompleted) GreenPrimary else Color(0xFFE0E0E0),
                    thickness = 2.dp
                )
            }

            // دائرة الخطوة
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 32.dp else 28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> GreenPrimary
                            isCurrent   -> GreenPrimary
                            else        -> Color(0xFFE0E0E0)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Outlined.Check,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        "${index + 1}",
                        color      = if (isCurrent) Color.White else Color(0xFF999999),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp
                    )
                }
            }
        }
    }
}

// ─── بانر الترحيب ─────────────────────────────────────────────────────────────

@Composable
private fun WorkerPostBanner(workerName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF388E3C), GreenPrimary)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("👷", fontSize = 26.sp)
            }
            Column {
                Text(
                    if (workerName.isNotBlank()) "Hello, $workerName!" else "Hello, Worker!",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Let farmers know you're available\nand what services you offer",
                    color      = Color.White.copy(alpha = 0.85f),
                    fontSize   = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─── رأس الخطوة ────────────────────────────────────────────────────────────────

@Composable
private fun StepHeader(step: Int, title: String, subtitle: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(GreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$step",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
        }
        Column {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize   = 17.sp,
                color      = Color(0xFF1A1A1A)
            )
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF888888))
        }
    }
}

// ─── بطاقة القسم ──────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape           = RoundedCornerShape(18.dp),
        color           = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                }
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = Color(0xFF1A1A1A)
                )
            }
            HorizontalDivider(color = Color(0xFFF0F0F0))
            content()
        }
    }
}

// ─── حقل النموذج ──────────────────────────────────────────────────────────────

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isError: Boolean  = false,
    errorText: String = ""
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, fontSize = 13.sp) },
        placeholder   = { Text(placeholder, color = Color(0xFFAAAAAA), fontSize = 13.sp) },
        leadingIcon   = { Icon(icon, null, tint = GreenPrimary) },
        isError       = isError,
        supportingText = if (isError) {
            { Text(errorText, fontSize = 11.sp) }
        } else null,
        singleLine = true,
        shape      = RoundedCornerShape(14.dp),
        colors     = workerFieldColors(),
        modifier   = Modifier.fillMaxWidth()
    )
}

// ─── شبكة المهارات ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsGrid(
    skills: List<String>,
    selectedSkills: List<String>,
    onToggle: (String) -> Unit
) {
    val rows = skills.chunked(2)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { rowSkills ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowSkills.forEach { skill ->
                    val isSelected = skill in selectedSkills
                    Surface(
                        onClick  = { onToggle(skill) },
                        shape    = RoundedCornerShape(24.dp),
                        color    = if (isSelected) GreenPrimary else Color(0xFFF5F5F5),
                        border   = if (isSelected) null else BorderStroke(1.dp, Color(0xFFDDDDDD)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check, null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                skill,
                                fontSize   = 12.sp,
                                color      = if (isSelected) Color.White else Color(0xFF444444),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                // لو الصف فيه عنصر واحد فقط أضف spacer
                if (rowSkills.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ─── بطاقة معاينة المنشور ─────────────────────────────────────────────────────

@Composable
private fun PostPreviewCard(
    title: String,
    description: String,
    skills: List<String>,
    location: String,
    availability: String,
    expectedSalary: String,
    workerName: String
) {
    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = Color.White,
        border = BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // رأس البطاقة
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Visibility,
                    null,
                    tint     = GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Preview — How farmers will see your post",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = GreenPrimary
                )
            }

            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 10.dp),
                color     = Color(0xFFF0F0F0)
            )

            // اسم العامل
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        workerName.firstOrNull()?.toString() ?: "W",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        workerName.ifBlank { "Worker" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        color      = Color(0xFF1A1A1A)
                    )
                    Text("Worker", fontSize = 11.sp, color = Color(0xFF888888))
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GreenPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        "Active",
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize   = 11.sp,
                        color      = GreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // العنوان
            Text(
                title.ifBlank { "Your post title" },
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = if (title.isBlank()) Color(0xFFBBBBBB) else Color(0xFF1A1A1A)
            )

            // الوصف
            if (description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    description.take(120) + if (description.length > 120) "..." else "",
                    fontSize   = 13.sp,
                    color      = Color(0xFF555555),
                    lineHeight = 19.sp
                )
            }

            // المهارات
            if (skills.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    skills.take(3).forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GreenPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                skill,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color    = GreenPrimary
                            )
                        }
                    }
                    if (skills.size > 3) {
                        Text(
                            "+${skills.size - 3} more",
                            fontSize = 11.sp,
                            color    = Color(0xFF888888),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            // الموقع والراتب
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (location.isNotBlank()) {
                    Text("📍 $location", fontSize = 12.sp, color = Color(0xFF666666))
                }
                if (expectedSalary.isNotBlank()) {
                    Text("💰 $expectedSalary DA/day", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                }
                if (availability.isNotBlank()) {
                    Text("⏰ $availability", fontSize = 12.sp, color = Color(0xFF666666))
                }
            }
        }
    }
}

// ─── أزرار التنقل بين الخطوات ─────────────────────────────────────────────────

@Composable
private fun StepNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    canGoNext: Boolean,
    isLoading: Boolean,
    showSuccess: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPublish: () -> Unit
) {
    val isLastStep = currentStep == totalSteps - 1

    Surface(
        color           = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // زر رجوع
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onBack,
                    shape   = RoundedCornerShape(14.dp),
                    border  = BorderStroke(1.5.dp, GreenPrimary),
                    modifier = Modifier
                        .weight(0.4f)
                        .height(52.dp)
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack, null,
                        tint     = GreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Back", color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            // زر التالي / النشر
            Button(
                onClick  = if (isLastStep) onPublish else onNext,
                enabled  = canGoNext && !isLoading,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = GreenPrimary,
                    disabledContainerColor = GreenPrimary.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .weight(if (currentStep > 0) 0.6f else 1f)
                    .height(52.dp)
            ) {
                AnimatedContent(
                    targetState = when {
                        isLoading   -> "loading"
                        showSuccess -> "success"
                        isLastStep  -> "publish"
                        else        -> "next"
                    },
                    label = "btn_state"
                ) { state ->
                    when (state) {
                        "loading" -> Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                strokeWidth = 2.dp,
                                modifier    = Modifier.size(20.dp)
                            )
                            Text("Publishing...", fontWeight = FontWeight.SemiBold)
                        }
                        "success" -> Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Text("Published!", fontWeight = FontWeight.SemiBold)
                        }
                        "publish" -> Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Publish, null, modifier = Modifier.size(20.dp))
                            Text("Publish Post", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        else -> Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Next", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── ألوان الحقول ─────────────────────────────────────────────────────────────

@Composable
private fun workerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = GreenPrimary,
    unfocusedBorderColor    = Color(0xFFE5E5E5),
    focusedLabelColor       = GreenPrimary,
    unfocusedLabelColor     = Color(0xFF888888),
    focusedContainerColor   = Color.White,
    unfocusedContainerColor = Color(0xFFFAFAFA),
    cursorColor             = GreenPrimary
)
