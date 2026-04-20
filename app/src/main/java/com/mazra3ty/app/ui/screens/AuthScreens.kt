package com.mazra3ty.app.ui.screens

import android.util.Log
import androidx.compose.ui.tooling.preview.Preview
import com.mazra3ty.app.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import io.github.jan.supabase.postgrest.from
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.mazra3ty.app.ui.theme.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import com.mazra3ty.app.database.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ─────────────────────────────────────────────────────────────────────────────
// Auth steps
// Flow A (existing user): ONBOARDING → LOGIN → HOME
// Flow B (new user):      ONBOARDING → LOGIN → REGISTER → OTP → HOME
// ─────────────────────────────────────────────────────────────────────────────
enum class AuthStep { ONBOARDING, LOGIN, REGISTER, OTP ,HOME}
@Serializable
data class UserDto(
    val id: String,
    val email: String? = null,
    val role: String? = "worker",
    val is_deleted: Boolean? = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Root host
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AuthHost() {

    var step      by remember { mutableStateOf<AuthStep?>(null) } // 👈 null = loading
    var userEmail by rememberSaveable { mutableStateOf("") }
    var userRole  by rememberSaveable { mutableStateOf("worker") }

    val client = SupabaseClientProvider.client
    val scope  = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        val user = client.auth.currentUserOrNull()

        if (user != null) {

            userEmail = user.email ?: ""

            try {
                val users = client
                    .from("users")
                    .select {
                        filter {
                            eq("id", user.id)
                            eq("is_deleted", false)
                        }
                    }
                    .decodeAs<List<UserDto>>()

                val userData = users.firstOrNull()
                if (userData == null) {
                    // user is deleted
                    client.auth.signOut()
                    step = AuthStep.LOGIN
                    return@LaunchedEffect
                }
                userRole = userData?.role ?: "worker"

                Log.d("AUTH_DEBUG", "ROLE FROM DB = $userRole")

            } catch (e: Exception) {
                Log.e("AUTH_DEBUG", "ERROR = ${e.message}")
                userRole = "worker"
            }

            step = AuthStep.HOME

        } else {
            step = AuthStep.ONBOARDING
        }
    }
    // 🔄 Loading state (أثناء التحقق من session)
    if (step == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // 🔥 UI NAVIGATION
    AnimatedContent(
        targetState = step!!,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal)
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            else
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
        },
        label = "auth_nav"
    ) { current ->

        when (current) {

            // ───────── ONBOARDING ─────────
            AuthStep.ONBOARDING -> OnboardingScreen {
                step = AuthStep.LOGIN
            }

            // ───────── LOGIN ─────────
            AuthStep.LOGIN -> LoginScreen(
                client = client,
                onLoginSuccess = { email, role ->
                    userEmail = email
                    userRole  = role
                    step = AuthStep.HOME
                },
                onGoRegister = {
                    step = AuthStep.REGISTER
                }
            )

            // ───────── REGISTER ─────────
            AuthStep.REGISTER -> RegisterScreen(
                client = client,
                onRegisterSuccess = { email, role ->
                    userEmail = email
                    userRole  = role
                    step = AuthStep.OTP
                },
                onBack = {
                    step = AuthStep.LOGIN
                }
            )

            // ───────── OTP ─────────
            AuthStep.OTP -> OtpScreen(
                email = userEmail,
                client = client,
                onVerified = {
                    step = AuthStep.HOME
                },
                onBack = {
                    step = AuthStep.REGISTER
                }
            )

            AuthStep.HOME -> {
                val user = client.auth.currentUserOrNull()
                HomeScreen(
                    userId = user?.id ?: "",
                    userEmail = userEmail,
                    userRole = userRole,
                    onLogout = { scope.launch { client.auth.signOut(); step = AuthStep.LOGIN } }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONBOARDING SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFD4EDBB), Color(0xFFEAF3DA), BackgroundLight))
            )
    ) {
        Box(
            Modifier.size(260.dp).offset((-60).dp, (-60).dp)
                .background(GreenPrimary.copy(0.10f), CircleShape)
        )
        Box(
            Modifier.size(180.dp).align(Alignment.TopEnd).offset(50.dp, 40.dp)
                .background(GreenLight.copy(0.15f), CircleShape)
        )

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.42f),
                contentAlignment = Alignment.Center
            ) {
                this@Column.AnimatedVisibility(
                    visible = visible,
                    enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy), 0.6f) + fadeIn(tween(500))
                ) {
                    Image(
                        painter            = painterResource(R.mipmap.cercule_of_concept),
                        contentDescription = "Hero",
                        modifier           = Modifier.size(260.dp)
                    )
                }
            }

            Surface(
                modifier        = Modifier.fillMaxSize(),
                shape           = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                color           = SurfaceWhite,
                shadowElevation = 24.dp,
                tonalElevation  = 4.dp
            ) {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = visible,
                        enter   = slideInVertically { it / 2 } + fadeIn(tween(400, 200))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LogoMark(80.dp)
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Your farm, connected.",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign  = TextAlign.Center,
                                    lineHeight = 30.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Find skilled workers, discover job\nopportunities, and grow together.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign  = TextAlign.Center,
                                    lineHeight = 22.sp
                                ),
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(32.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier              = Modifier.fillMaxWidth()
                            ) {
                                FeaturePill(Icons.Outlined.Agriculture, "Workers", Modifier.weight(1f))
                                FeaturePill(Icons.Outlined.Yard,        "Farmers", Modifier.weight(1f))
                                FeaturePill(Icons.Outlined.Work,        "Jobs",    Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(32.dp))
                            GreenButton("Get Started", icon = Icons.Outlined.ArrowForward) { onContinue() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = SoftGreenBg,
        border   = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.18f))
    ) {
        Column(
            modifier            = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = GreenPrimaryDark, modifier = Modifier.size(20.dp))
            Text(
                label,
                style      = MaterialTheme.typography.labelSmall,
                color      = GreenPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOGIN SCREEN  — email + password → signInWith(Email)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(
    client:         SupabaseClient,
    onLoginSuccess: (email: String, role: String) -> Unit,
    onGoRegister:   () -> Unit
) {
    var email    by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPw   by rememberSaveable { mutableStateOf(false) }
    var loading  by rememberSaveable { mutableStateOf(false) }
    var error    by rememberSaveable { mutableStateOf<String?>(null) }
    val scope    = rememberCoroutineScope()
    val focusMgr = LocalFocusManager.current

    val emailOk  = remember(email) { android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val canLogin = emailOk && password.isNotBlank() && !loading

    fun doLogin() {
        if (!canLogin) return
        focusMgr.clearFocus(); loading = true; error = null
        scope.launch {
            try {
                client.auth.signInWith(Email) {
                    this.email    = email.trim()
                    this.password = password
                }
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    val users = client
                        .from("users")
                        .select { filter { eq("id", user.id) } }
                        .decodeAs<List<UserDto>>()
                    val role = users.firstOrNull()?.role ?: "worker"
                    onLoginSuccess(email.trim(), role)
                } else {
                    error = "Sign-in failed. Please try again."
                }
            } catch (e: Exception) {
                error = when {
                    e.message?.contains("Invalid login", ignoreCase = true) == true -> "Incorrect email or password"
                    e.message?.contains("network",       ignoreCase = true) == true -> "No internet connection"
                    e.message?.contains("rate",          ignoreCase = true) == true -> "Too many attempts – try later"
                    else -> "Sign-in failed. Please try again."
                }
            } finally { loading = false }
        }
    }

    AuthScaffold(step = 1, totalSteps = 3, onBack = null) {
        AuthStepIcon(Icons.Outlined.LockOpen, GreenPrimary)
        Spacer(Modifier.height(20.dp))
        Text(
            "Welcome back",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Sign in to your Mazra3ty account",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(32.dp))

        AppTextField(
            value         = email,
            onValueChange = { email = it; error = null },
            label         = "Email address",
            icon          = Icons.Outlined.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next
        )
        Spacer(Modifier.height(14.dp))

        AppTextField(
            value         = password,
            onValueChange = { password = it; error = null },
            label         = "Password",
            icon          = Icons.Outlined.Lock,
            keyboardType  = KeyboardType.Password,
            imeAction     = ImeAction.Done,
            isPassword    = true,
            showPassword  = showPw,
            onTogglePw    = { showPw = !showPw },
            onImeAction   = { doLogin() }
        )
        Spacer(Modifier.height(12.dp))

        ErrorBannerSlot(error) { error = null }

        Spacer(Modifier.height(24.dp))
        GreenButton(
            text    = if (loading) "Signing in…" else "Sign In",
            enabled = canLogin,
            icon    = if (loading) null else Icons.Outlined.Login
        ) { doLogin() }

        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = GrayLight)
            Text("  New to Mazra3ty?  ", style = MaterialTheme.typography.labelSmall, color = GrayMedium)
            HorizontalDivider(Modifier.weight(1f), color = GrayLight)
        }
        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick  = onGoRegister,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
            border   = BorderStroke(1.5.dp, GreenPrimary),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
        ) {
            Icon(Icons.Outlined.PersonAdd, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Create Account",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.weight(1f))
        TermsText()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REGISTER SCREEN — full profile + signUpWith(Email)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    client:            SupabaseClient,
    onRegisterSuccess: (email: String, role: String) -> Unit,
    onBack:            () -> Unit
) {
    var fullName  by rememberSaveable { mutableStateOf("") }
    var email     by rememberSaveable { mutableStateOf("") }
    var phone     by rememberSaveable { mutableStateOf("") }
    var password  by rememberSaveable { mutableStateOf("") }
    var confirmPw by rememberSaveable { mutableStateOf("") }
    var dob       by rememberSaveable { mutableStateOf("") }
    var bio       by rememberSaveable { mutableStateOf("") }
    var role      by rememberSaveable { mutableStateOf("worker") }
    var showPw    by rememberSaveable { mutableStateOf(false) }
    var showCPw   by rememberSaveable { mutableStateOf(false) }
    var loading   by rememberSaveable { mutableStateOf(false) }
    var errorMsg  by rememberSaveable { mutableStateOf<String?>(null) }
    val scope     = rememberCoroutineScope()

    val emailError = email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val pwShort    = password.isNotBlank() && password.length < 6
    val pwMismatch = confirmPw.isNotBlank() && confirmPw != password
    val dobError   = dob.isNotBlank() && !dob.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
    val canSubmit  = fullName.isNotBlank() && email.isNotBlank() && !emailError &&
            password.length >= 6 && !pwMismatch && (dob.isBlank() || !dobError) && !loading

    fun doRegister() {
        if (!canSubmit) return
        scope.launch {
            loading = true; errorMsg = null
            try {
                client.auth.signUpWith(Email) {
                    this.email    = email.trim()
                    this.password = password
                    data = buildJsonObject {
                        put("full_name",     fullName.trim())
                        put("phone",         phone.trim())
                        put("role",          role)
                        put("date_of_birth", dob.trim())
                        put("bio",           bio.trim())
                    }
                }
                onRegisterSuccess(email.trim(), role)
            } catch (e: Exception) {
                errorMsg = when {
                    e.message?.contains("already registered", ignoreCase = true) == true ->
                        "Email already registered. Please sign in."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "No internet connection"
                    else -> e.message ?: "Registration failed. Please try again."
                }
            } finally { loading = false }
        }
    }

    AuthScaffold(step = 2, totalSteps = 3, onBack = onBack) {
        AuthStepIcon(Icons.Outlined.PersonAdd, GreenPrimary)
        Spacer(Modifier.height(16.dp))
        Text(
            "Create your account",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Fill in your details to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(24.dp))

        ErrorBannerSlot(errorMsg) { errorMsg = null }
        if (errorMsg != null) Spacer(Modifier.height(12.dp))

        // ── Personal Information ──
        FormSectionLabel("Personal Information")
        Spacer(Modifier.height(10.dp))
        AppTextField(fullName, { fullName = it }, "Full name ",               Icons.Outlined.Person)
        Spacer(Modifier.height(12.dp))
        AppTextField(phone,    { phone = it },    "Phone ",           Icons.Outlined.Phone, keyboardType = KeyboardType.Phone)
        Spacer(Modifier.height(12.dp))

        var showDatePicker by remember { mutableStateOf(false) }
        var dob by remember { mutableStateOf("") }
        Box(
            modifier = Modifier.clickable { showDatePicker = true }
        ) {
            AppTextField(
                value = dob,
                onValueChange = {},
                label = "Date of birth",
                icon = Icons.Outlined.CalendarMonth,
                enabled = false // يمنع الكتابة
            )
        }
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false

                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.text.SimpleDateFormat("yyyy-MM-dd")
                                .format(java.util.Date(millis))
                            dob = date
                        }
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.height(20.dp))

        // ── Account Credentials ──
        FormSectionLabel("Account Credentials")
        Spacer(Modifier.height(10.dp))
        AppTextField(email,     { email = it },     "Email address",           Icons.Outlined.Email,    keyboardType = KeyboardType.Email,    isError = emailError, errorMsg = "Enter a valid email")
        Spacer(Modifier.height(12.dp))
        AppTextField(password,  { password = it },  "Password",  Icons.Outlined.Lock,     keyboardType = KeyboardType.Password, isPassword = true, showPassword = showPw,  onTogglePw = { showPw  = !showPw  }, isError = pwShort,    errorMsg = "At least 6 characters")
        Spacer(Modifier.height(12.dp))
        AppTextField(confirmPw, { confirmPw = it }, "Confirm password ",        Icons.Outlined.LockOpen, keyboardType = KeyboardType.Password, isPassword = true, showPassword = showCPw, onTogglePw = { showCPw = !showCPw }, isError = pwMismatch, errorMsg = "Passwords do not match")
        Spacer(Modifier.height(20.dp))

        // ── Role ──
        FormSectionLabel("I am a…")
        Spacer(Modifier.height(10.dp))
        RoleSelector(role) { role = it }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick  = ::doRegister,
            enabled  = canSubmit,
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = GreenPrimary,
                disabledContainerColor = GreenPrimary.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Outlined.HowToReg, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Account", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        TermsText()
        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OTP SCREEN — 8-digit email confirmation
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OtpScreen(
    email:      String,
    client:     SupabaseClient,
    onVerified: () -> Unit,
    onBack:     () -> Unit
) {
    var otp     by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error   by rememberSaveable { mutableStateOf<String?>(null) }
    var sent    by rememberSaveable { mutableStateOf(true) } // Supabase auto-sends on signUpWith
    val scope   = rememberCoroutineScope()

    fun doVerify() {
        if (otp.length != 8) return
        loading = true; error = null
        scope.launch {
            try {
                client.auth.verifyEmailOtp(
                    type  = OtpType.Email.EMAIL,
                    email = email.trim(),
                    token = otp.trim()
                )
                onVerified()
            } catch (e: Exception) {
                error = when {
                    e.message?.contains("invalid",  ignoreCase = true) == true -> "Invalid or expired code"
                    e.message?.contains("network",  ignoreCase = true) == true -> "No internet connection"
                    else -> "Verification failed. Please try again."
                }
            } finally { loading = false }
        }
    }

    fun doResend() {
        otp = ""; error = null; sent = false
        scope.launch {
            loading = true
            try {
                client.auth.resendEmail(

                    type  = OtpType.Email.EMAIL,
                    email = email.trim()
                )
                sent = true
            } catch (e: Exception) {
                error = "Resend failed: ${e.message}"
            } finally { loading = false }
        }
    }

    AuthScaffold(step = 3, totalSteps = 3, onBack = onBack) {
        AuthStepIcon(Icons.Outlined.MarkEmailRead, GreenPrimary)
        Spacer(Modifier.height(20.dp))
        Text(
            "Check your email",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "We sent an 8-digit confirmation code to\n$email",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = TextSecondary
        )
        Spacer(Modifier.height(32.dp))

        // ── 8-digit OTP input ──
        OutlinedTextField(
            value         = otp,
            onValueChange = {
                if (it.length <= 8 && it.all(Char::isDigit)) { otp = it; error = null }
            },
            modifier      = Modifier.fillMaxWidth(),
            label         = {
                Text(
                    "8-digit confirmation code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            placeholder   = {
                Text(
                    "• • • •  • • • •",
                    color = GrayMedium,
                    style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 4.sp)
                )
            },
            leadingIcon   = {
                Icon(
                    Icons.Outlined.Key, null,
                    tint = if (otp.isNotBlank()) GreenPrimary else GrayMedium
                )
            },
            trailingIcon  = {
                AnimatedVisibility(
                    otp.length == 8,
                    enter = scaleIn() + fadeIn(),
                    exit  = scaleOut() + fadeOut()
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { doVerify() }),
            singleLine    = true,
            shape         = RoundedCornerShape(14.dp),
            textStyle     = MaterialTheme.typography.bodyLarge.copy(
                color         = TextPrimary,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 6.sp,
                textAlign     = TextAlign.Center
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor          = TextPrimary,
                unfocusedTextColor        = TextPrimary,
                disabledTextColor         = TextPrimary.copy(alpha = 0.50f),
                errorTextColor            = TextPrimary,
                focusedLabelColor         = GreenPrimary,
                unfocusedLabelColor       = TextPrimary,
                focusedBorderColor        = GreenPrimary,
                unfocusedBorderColor      = GrayLight,
                focusedContainerColor     = Color.Transparent,
                unfocusedContainerColor   = Color.Transparent,
                disabledContainerColor    = Color.Transparent,
                cursorColor               = GreenPrimary,
                focusedLeadingIconColor   = GreenPrimary,
                unfocusedLeadingIconColor = GrayMedium,
                focusedPlaceholderColor   = GrayMedium,
                unfocusedPlaceholderColor = GrayMedium
            )
        )

        // Progress dots
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier              = Modifier.fillMaxWidth()
        ) {
            repeat(8) { i ->
                val filled = i < otp.length
                val color  by animateColorAsState(
                    if (filled) GreenPrimary else GrayLight,
                    tween(150),
                    "dot$i"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(color, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        ErrorBannerSlot(error) { error = null }

        if (sent && !loading) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = GreenPrimaryDark, modifier = Modifier.size(14.dp))
                Text(
                    "Code sent – check your inbox",
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenPrimaryDark
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        GreenButton(
            text    = if (loading) "Verifying…" else "Verify & Continue",
            enabled = otp.length == 8 && !loading,
            icon    = if (loading) null else Icons.Outlined.Verified
        ) { doVerify() }

        Spacer(Modifier.height(14.dp))

        TextButton(onClick = ::doResend, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Didn't receive the code? Resend",
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimaryDark
            )
        }

        Spacer(Modifier.weight(1f))
        TermsText()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FARMER DASHBOARD
// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────
// WORKER DASHBOARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WorkerDashboard(userEmail: String, onLogout: () -> Unit) {
    val stats = listOf(
        Triple(Icons.Outlined.Work,        "Applied Jobs", "5"),
        Triple(Icons.Outlined.CheckCircle, "Accepted",     "2"),
        Triple(Icons.Outlined.StarOutline, "My Rating",    "4.6"),
        Triple(Icons.Outlined.Assignment,  "Completed",    "14")
    )
    DashboardScaffold(
        title     = "Worker Dashboard",
        subtitle  = "Find your next opportunity",
        email     = userEmail,
        roleColor = GreenPrimary,
        roleLabel = "Farm Worker",
        roleIcon  = Icons.Outlined.Agriculture,
        onLogout  = onLogout
    ) {
        Text(
            "Overview",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            stats.take(2).forEach { (icon, label, value) -> StatCard(icon, label, value, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            stats.drop(2).forEach { (icon, label, value) -> StatCard(icon, label, value, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(28.dp))

        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        DashboardAction(Icons.Outlined.Search,        "Browse Jobs",       "Find farm work near you")
        Spacer(Modifier.height(10.dp))
        DashboardAction(Icons.Outlined.PostAdd,       "Post My Profile",   "Let farmers discover you")
        Spacer(Modifier.height(10.dp))
        DashboardAction(Icons.Outlined.Notifications, "My Applications",   "Track your job applications")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Dashboard scaffold
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashboardScaffold(
    title:     String,
    subtitle:  String,
    email:     String,
    roleColor: Color,
    roleLabel: String,
    roleIcon:  ImageVector,
    onLogout:  () -> Unit,
    content:   @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFD4EDBB), Color(0xFFEAF3DA), BackgroundLight)))
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                LogoMark(36.dp)
                IconButton(
                    onClick  = onLogout,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceWhite.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(Icons.Outlined.Logout, "Logout", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                shape  = RoundedCornerShape(10.dp),
                color  = roleColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, roleColor.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(roleIcon, null, tint = roleColor, modifier = Modifier.size(14.dp))
                    Text(
                        roleLabel,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = roleColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(email,    style = MaterialTheme.typography.labelSmall,  color = GrayMedium)
        }

        // Floating content card
        Surface(
            modifier        = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.68f),
            shape           = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color           = BackgroundLight,
            shadowElevation = 16.dp,
            tonalElevation  = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .verticalScroll(rememberScrollState()),
                content  = content
            )
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(16.dp),
        color           = SurfaceWhite,
        shadowElevation = 4.dp,
        tonalElevation  = 1.dp,
        border          = BorderStroke(1.dp, GrayLight)
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun DashboardAction(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(14.dp),
        color           = SurfaceWhite,
        shadowElevation = 2.dp,
        border          = BorderStroke(1.dp, GrayLight)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier.size(42.dp).background(SoftGreenBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = GreenPrimaryDark, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style      = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color      = TextPrimary
                )
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = GrayMedium, modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Role selector
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RoleSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "worker" to (Icons.Outlined.Agriculture to "Farm Worker"),
        "farmer" to (Icons.Outlined.Yard        to "Farmer / Owner")
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (key, pair) ->
            val (icon, label) = pair
            val picked        = selected == key
            val bgColor by animateColorAsState(
                if (picked) GreenPrimary.copy(alpha = 0.10f) else Color.Transparent,
                tween(200),
                "role_bg"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (picked) GreenPrimary else GrayLight,
                        RoundedCornerShape(12.dp)
                    )
                    .background(bgColor)
                    .clickable { onSelect(key) }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon, null,
                    tint     = if (picked) GreenPrimary else GrayMedium,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    label,
                    style      = MaterialTheme.typography.labelLarge,
                    color      = if (picked) GreenPrimary else TextPrimary,
                    fontWeight = if (picked) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CRITICAL: AppTextField — every colour state uses TextPrimary for text/label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    icon:          ImageVector,
    keyboardType:  KeyboardType  = KeyboardType.Text,
    imeAction:     ImeAction     = ImeAction.Next,
    isPassword:    Boolean       = false,
    showPassword:  Boolean       = false,
    onTogglePw:    (() -> Unit)? = null,
    onImeAction:   (() -> Unit)? = null,
    isError:       Boolean       = false,
    errorMsg:      String?       = null,
    singleLine:    Boolean       = true,
    minLines:      Int           = 1,
    enabled:       Boolean       = true,
    modifier:      Modifier      = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value            = value,
            onValueChange    = onValueChange,
            enabled          = enabled,
            label            = {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    // Always TextPrimary so label is visible in both focused and unfocused states
                    color = when {
                        isError              -> RedError
                        else                 -> TextPrimary
                    }
                )
            },
            leadingIcon      = {
                Icon(
                    icon, null,
                    tint     = when {
                        isError            -> RedError
                        value.isNotBlank() -> GreenPrimary
                        else               -> GrayMedium
                    },
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon     = if (isPassword && onTogglePw != null) ({
                IconButton(onClick = onTogglePw) {
                    Icon(
                        if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        null,
                        tint     = GrayMedium,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }) else null,
            visualTransformation = if (isPassword && !showPassword)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions  = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions  = KeyboardActions(
                onNext = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() }
            ),
            isError          = isError,
            singleLine       = singleLine,
            minLines         = minLines,
            shape            = RoundedCornerShape(14.dp),
            // ── ALL colour slots explicitly set ──────────────────────────────
            colors           = OutlinedTextFieldDefaults.colors(
                // Text
                focusedTextColor          = TextPrimary,
                unfocusedTextColor        = TextPrimary,
                disabledTextColor         = TextPrimary.copy(alpha = 0.50f),
                errorTextColor            = TextPrimary,
                // Label
                focusedLabelColor         = GreenPrimary,
                unfocusedLabelColor       = TextPrimary,      // ← KEY FIX
                disabledLabelColor        = TextPrimary.copy(alpha = 0.50f),
                errorLabelColor           = RedError,
                // Border
                focusedBorderColor        = GreenPrimary,
                unfocusedBorderColor      = GrayLight,
                disabledBorderColor       = GrayLight.copy(alpha = 0.50f),
                errorBorderColor          = RedError,
                // Container
                focusedContainerColor     = Color.Transparent,
                unfocusedContainerColor   = Color.Transparent,
                disabledContainerColor    = Color.Transparent,
                errorContainerColor       = Color.Transparent,
                // Cursor
                cursorColor               = GreenPrimary,
                errorCursorColor          = RedError,
                // Icons
                focusedLeadingIconColor   = GreenPrimary,
                unfocusedLeadingIconColor = GrayMedium,
                disabledLeadingIconColor  = GrayMedium.copy(alpha = 0.50f),
                errorLeadingIconColor     = RedError,
                focusedTrailingIconColor  = GreenPrimary,
                unfocusedTrailingIconColor= GrayMedium,
                // Placeholder
                focusedPlaceholderColor   = GrayMedium,
                unfocusedPlaceholderColor = GrayMedium
            ),
            // textStyle also pins color so it's never overridden by theme
            textStyle        = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            modifier         = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(isError && errorMsg != null) {
            errorMsg?.let {
                Text(
                    it,
                    color    = RedError,
                    style    = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 14.dp, top = 3.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared small components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormSectionLabel(text: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.width(3.dp).height(14.dp).background(GreenPrimary, RoundedCornerShape(2.dp)))
        Text(
            text,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )
    }
}

@Composable
private fun ErrorBannerSlot(message: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = message != null,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically()
    ) {
        message?.let {
            Surface(
                color    = RedError.copy(alpha = 0.08f),
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.dp, RedError.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = RedError, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        it,
                        color      = RedError,
                        style      = MaterialTheme.typography.bodyMedium,
                        modifier   = Modifier.weight(1f),
                        lineHeight = 20.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Outlined.Close, null, tint = RedError, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AuthStepIcon(icon: ImageVector, tint: Color) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
        Box(
            modifier         = Modifier.size(60.dp).background(tint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun GreenButton(
    text:    String,
    enabled: Boolean      = true,
    icon:    ImageVector? = null,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (enabled) 1f else 0.97f, spring(stiffness = Spring.StiffnessMedium), label = "btn")
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp).scale(scale),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = GreenPrimary,
            disabledContainerColor = GreenPrimary.copy(alpha = 0.35f)
        )
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TermsText() {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = TextSecondary)) { append("By continuing, you agree to our\n") }
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = GreenPrimaryDark)) { append("Terms & Conditions") }
            withStyle(SpanStyle(color = TextSecondary)) { append("  ·  ") }
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = GreenPrimaryDark)) { append("Privacy Policy") }
        },
        style      = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
        modifier   = Modifier.fillMaxWidth(),
        lineHeight = 22.sp
    )
}

@Composable
fun LogoMark(size: Dp) {
    Image(painterResource(R.drawable.logo), "Logo", Modifier.size(size))
}

@Composable
fun AuthScaffold(
    step:       Int,
    totalSteps: Int,
    onBack:     (() -> Unit)? = null,
    content:    @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFD4EDBB), Color(0xFFEAF3DA), BackgroundLight)))
    ) {
        Box(
            Modifier.size(200.dp).align(Alignment.TopEnd)
                .offset(60.dp, (-40).dp)
                .background(GreenLight.copy(0.18f), CircleShape)
        )

        if (onBack != null) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .align(Alignment.TopStart),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier.size(40.dp).background(SurfaceWhite.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                StepDots(step, totalSteps)
            }
        } else {
            Row(
                modifier              = Modifier.align(Alignment.TopEnd).padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) { StepDots(step, totalSteps) }
        }

        Surface(
            modifier        = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 72.dp),
            shape           = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            color           = BackgroundLight,
            shadowElevation = 12.dp,
            tonalElevation  = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState()),
                content  = content
            )
        }
    }
}

@Composable
private fun StepDots(step: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { idx ->
            val active = idx + 1 <= step
            val width by animateDpAsState(
                if (idx + 1 == step) 24.dp else 8.dp,
                spring(stiffness = Spring.StiffnessMediumLow),
                "dot"
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .background(if (active) GreenPrimary else GrayLight, CircleShape)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable fun PreviewOnboarding() { Mazra3tyTheme { OnboardingScreen {} } }

@Preview(showBackground = true, showSystemUi = true)
@Composable fun PreviewLogin() { Mazra3tyTheme { LoginScreen(SupabaseClientProvider.client, { _, _ -> }, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable fun PreviewRegister() { Mazra3tyTheme { RegisterScreen(SupabaseClientProvider.client, { _, _ -> }, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable fun PreviewOtp() { Mazra3tyTheme { OtpScreen("user@email.com", SupabaseClientProvider.client, {}, {}) } }