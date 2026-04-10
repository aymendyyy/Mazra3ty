package com.mazra3ty.app.ui.screens

import androidx.compose.ui.tooling.preview.Preview
import com.mazra3ty.app.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.mazra3ty.app.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────
// Navigation enum
// ─────────────────────────────────────────────
enum class AuthStep { ONBOARDING, EMAIL, OTP, REGISTER }

// ─────────────────────────────────────────────
// Root host – swap between steps
// ─────────────────────────────────────────────
@Composable
fun AuthHost() {
    var step by remember { mutableStateOf(AuthStep.ONBOARDING) }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "auth_transition"
    ) { current ->
        when (current) {
            AuthStep.ONBOARDING -> OnboardingScreen { step = AuthStep.EMAIL }
            AuthStep.EMAIL      -> EmailScreen(
                onContinue = { step = AuthStep.OTP },
                onBack     = { step = AuthStep.ONBOARDING }
            )
            AuthStep.OTP        -> OtpScreen(
                onVerified = { step = AuthStep.REGISTER },
                onBack     = { step = AuthStep.EMAIL }
            )
            AuthStep.REGISTER   -> RegisterScreen(
                onDone = { /* navigate to main */ },
                onBack = { step = AuthStep.OTP }
            )
        }
    }
}

// ─────────────────────────────────────────────
// ONBOARDING SCREEN
// ─────────────────────────────────────────────
@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    var email by remember { mutableStateOf("") }

    // Subtle entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SoftGreenBg, Color(0xFFEAF3DA))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
            .padding(12.dp),
        contentAlignment = Alignment.Center // يخلي المحتوى في الوسط
        ) {
            Image(
                painter = painterResource(id = R.mipmap.cercule_of_concept),
                contentDescription = "Logo"

            )
        }

        // Illustration area
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {

        }

        // Bottom card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f),
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            color = BackgroundLight,
            shadowElevation = 16.dp,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoMark(size = 112.dp)

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "All your farming needs\nin one app!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    ),
                    color = TextPrimary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Find trusted workers, discover job opportunities,\nand connect with farmers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContinue() }
                ) {
                    GreenEmailField(
                        value = "",
                        onValueChange = {},
                        placeholder = "Enter your email",
                        onDone = {},
                        readOnly = true,
                        enabled = false
                    )
                }

                Spacer(Modifier.height(16.dp))

            }
        }
    }
}

// ─────────────────────────────────────────────
// EMAIL SCREEN
// ─────────────────────────────────────────────
@Composable
fun EmailScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }

    AuthScaffold(
        step = 1,
        totalSteps = 3,
        onBack = onBack
    ) {
        // Icon badge
        AuthStepIcon(icon = Icons.Outlined.MarkEmailUnread, tint = GreenPrimary)

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Enter your email",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "We'll send a 4-digit verification code to confirm your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(28.dp))

        GreenEmailField(
            value = email,
            onValueChange = { email = it },
            placeholder = "your@email.com",
            onDone = { if (email.isNotBlank()) onContinue() }
        )

        Spacer(Modifier.height(20.dp))

        GreenButton(
            text = "Send Code",
            enabled = email.isNotBlank(),
            icon = Icons.Outlined.Send
        ) { onContinue() }

        Spacer(Modifier.weight(1f))

        TermsText()
    }
}

// ─────────────────────────────────────────────
// OTP SCREEN
// ─────────────────────────────────────────────
@Composable
fun OtpScreen(onVerified: (String) -> Unit, onBack: () -> Unit) {

    val otpLength = 4
    var otp by remember { mutableStateOf(List(otpLength) { "" }) }
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { List(otpLength) { FocusRequester() } }
    val isComplete = otp.all { it.isNotEmpty() }
    val code = otp.joinToString("")

    // Resend countdown timer
    var secondsLeft by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        canResend = true
    }

    AuthScaffold(
        step = 2,
        totalSteps = 3,
        onBack = onBack
    ) {
        AuthStepIcon(icon = Icons.Outlined.PhonelinkLock, tint = GreenPrimary)

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Verify your email",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Enter the 4-digit code we just sent to your email.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(32.dp))

        // OTP boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(otpLength) { index ->
                OtpBoxPro(
                    value = otp[index],
                    focusRequester = focusRequesters[index],
                    onValueChange = { value ->
                        if (value.length > 1) {
                            val digits = value.filter { it.isDigit() }.take(otpLength)
                            otp = digits.map { it.toString() } +
                                    List(otpLength - digits.length) { "" }
                            when {
                                digits.length == otpLength -> {
                                    focusManager.clearFocus()
                                    onVerified(digits)
                                }
                                digits.isNotEmpty() -> focusRequesters[digits.length].requestFocus()
                            }
                        } else if (value.all { it.isDigit() }) {
                            otp = otp.toMutableList().also { it[index] = value }
                            if (value.isNotEmpty() && index < otpLength - 1)
                                focusRequesters[index + 1].requestFocus()
                            if (otp.all { it.isNotEmpty() }) {
                                focusManager.clearFocus()
                                onVerified(code)
                            }
                        }
                    },
                    onBackspace = {
                        if (otp[index].isNotEmpty()) {
                            otp = otp.toMutableList().also { it[index] = "" }
                        } else if (index > 0) {
                            otp = otp.toMutableList().also { it[index - 1] = "" }
                            focusRequesters[index - 1].requestFocus()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Resend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (canResend) Icons.Outlined.Refresh else Icons.Outlined.Timer,
                contentDescription = null,
                tint = if (canResend) GreenPrimary else GrayMedium,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            if (canResend) {
                Text(
                    text = "Resend code",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = GreenPrimary,
                    modifier = Modifier.clickable {
                        secondsLeft = 60
                        canResend = false
                        otp = List(otpLength) { "" }
                    }
                )
            } else {
                Text(
                    text = "Resend in ${secondsLeft}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        GreenButton(
            text = "Verify & Continue",
            enabled = isComplete,
            icon = Icons.Outlined.VerifiedUser
        ) { onVerified(code) }
    }
}

// ─────────────────────────────────────────────
// REGISTER SCREEN
// ─────────────────────────────────────────────
@Composable
fun RegisterScreen(onDone: () -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCountryCode by remember { mutableStateOf("🇩🇿 +213") }
    var expanded by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    val countryCodes = listOf(
        "🇩🇿 +213", "🇫🇷 +33", "🇸🇦 +966",
        "🇦🇪 +971", "🇲🇦 +212", "🇹🇳 +216",
        "🇺🇸 +1",  "🇬🇧 +44",  "🇹🇷 +90"
    )

    AuthScaffold(
        step = 3,
        totalSteps = 3,
        onBack = onBack
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(SoftGreenBg, CircleShape)
                .border(2.dp, GreenPrimary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Avatar",
                tint = GreenPrimary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Create your account",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Fill in your details to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        // Full name
        OutlinedInputField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Full name",
            leadingIcon = {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Name",
                    tint = if (name.isNotBlank()) GreenPrimary else GrayMedium
                )
            }
        )

        Spacer(Modifier.height(14.dp))

        // Phone + country code
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Country code picker
            Box {
                Surface(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GrayLight),
                    color = SurfaceWhite,
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedCountryCode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Icon(
                            Icons.Outlined.ArrowDropDown,
                            contentDescription = "Select country",
                            tint = GrayMedium,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    countryCodes.forEach { code ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedCountryCode = code
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedInputField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "Phone number",
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = "Phone",
                        tint = if (phone.isNotBlank()) GreenPrimary else GrayMedium
                    )
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        // Date of birth label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Cake,
                contentDescription = "Birthday",
                tint = GrayDark,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Date of birth",
                style = MaterialTheme.typography.labelLarge,
                color = GrayDark
            )
        }

        Spacer(Modifier.height(8.dp))

        // Date fields
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DatePartField(day,   { day = it },   "DD",   2, Modifier.weight(1f))
            DatePartField(month, { month = it }, "MM",   2, Modifier.weight(1f))
            DatePartField(year,  { year = it },  "YYYY", 4, Modifier.weight(1.6f))
        }

        Spacer(Modifier.height(28.dp))

        GreenButton(
            text = "Create Account",
            enabled = name.isNotBlank() && phone.isNotBlank(),
            icon = Icons.Outlined.HowToReg
        ) { onDone() }

        Spacer(Modifier.weight(1f))

        TermsText()
    }
}

// ═══════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════

/** Animated step icon badge */
@Composable
fun AuthStepIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(tint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Green branded primary button with trailing icon */
@Composable
fun GreenButton(
    text: String,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.97f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )
}

/** Email input with envelope icon */
@Composable
fun GreenEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onDone: () -> Unit = {},
    readOnly: Boolean = false,
    enabled:Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = GrayMedium, style = MaterialTheme.typography.bodyMedium)
        },
        readOnly=readOnly,
        enabled=enabled,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = "Email",
                tint = if (value.isNotBlank()) GreenPrimary else GrayMedium
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = value.isNotBlank(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Valid",
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)

                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = GreenPrimary,
            unfocusedBorderColor    = GrayLight,
            focusedContainerColor   = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            focusedLeadingIconColor = GreenPrimary
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
    )
}

/** Generic outlined input */
@Composable
fun OutlinedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = GrayMedium, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = leadingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = GreenPrimary,
            unfocusedBorderColor    = GrayLight,
            focusedContainerColor   = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            focusedLeadingIconColor = GreenPrimary
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
    )
}

/** DD / MM / YYYY date part box */
@Composable
fun DatePartField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLen: Int,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLen && it.all(Char::isDigit)) onValueChange(it) },
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                color = GrayMedium,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = GreenPrimary,
            unfocusedBorderColor    = GrayLight,
            focusedContainerColor   = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
    )
}

/** Enhanced OTP digit box */
@Composable
fun OtpBoxPro(
    value: String,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            value.isNotEmpty() -> GreenPrimary
            isFocused          -> GreenPrimaryDark
            else               -> GrayLight
        },
        animationSpec = tween(200),
        label = "otp_border"
    )

    val bgColor by animateColorAsState(
        targetValue = if (value.isNotEmpty()) SoftGreenBg else SurfaceWhite,
        animationSpec = tween(200),
        label = "otp_bg"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "otp_scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 6.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = GreenPrimary.copy(alpha = 0.2f),
                spotColor = GreenPrimary.copy(alpha = 0.25f)
            )
            .border(
                width = if (isFocused || value.isNotEmpty()) 2.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .background(bgColor, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .onKeyEvent {
                    if (it.key == Key.Backspace && it.type == KeyEventType.KeyDown) {
                        onBackspace()
                        true
                    } else false
                },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    innerTextField()
                }
            }
        )
    }
}

/** Reusable scaffold for auth inner screens with back button & step progress */
@Composable
fun AuthScaffold(
    step: Int,
    totalSteps: Int,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SoftGreenBg, Color(0xFFEAF3DA))
                )
            )
    ) {
        // Back button + step dots in the top area
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceWhite.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Step dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalSteps) { idx ->
                        val isActive = idx + 1 <= step
                        val width by animateDpAsState(
                            targetValue = if (idx + 1 == step) 24.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "dot_w"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .background(
                                    color = if (isActive) GreenPrimary else GrayLight,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }

        // White card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 72.dp),
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            color = BackgroundLight,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 32.dp)
                    .verticalScroll(rememberScrollState()),
                content = content
            )
        }
    }
}

/** Terms & Conditions bottom text */
@Composable
fun TermsText() {
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = TextSecondary)) {
            append("By continuing, you agree to our\n")
        }
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = GreenPrimaryDark)) {
            append("Terms & Conditions")
        }
        withStyle(SpanStyle(color = TextSecondary)) { append("  ·  ") }
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = GreenPrimaryDark)) {
            append("Privacy Policy")
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth()
    )
}

/** App logo */
@Composable
fun LogoMark(size: Dp) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "App Logo",
        modifier = Modifier.size(size)
    )
}

// ─────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewOnboarding() {
    Mazra3tyTheme { OnboardingScreen(onContinue = {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewEmail() {
    Mazra3tyTheme { EmailScreen(onContinue = {}, onBack = {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewOtp() {
    Mazra3tyTheme { OtpScreen(onVerified = {}, onBack = {}) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewRegister() {
    Mazra3tyTheme { RegisterScreen(onDone = {}, onBack = {}) }
}