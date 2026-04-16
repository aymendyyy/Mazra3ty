package com.mazra3ty.app.ui.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.R
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.Mazra3tyTheme
import com.mazra3ty.app.ui.theme.RedError
import com.mazra3ty.app.ui.theme.TextPrimary

// ─────────────────────────────────────────────────────────────────────────────
// Modern Admin Top Bar
// - Logo on LEFT
// - Animated page title
// - Logout on RIGHT
// - Bigger + cleaner UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdminTopBar(
    currentRoute: String,
    onLogout: () -> Unit
) {

    // Get current page title
    val pageTitle = adminScreens
        .firstOrNull { it.route == currentRoute }
        ?.label
        ?: "Admin"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp)
            .background(Color.White)
    ) {

        // ── Main Bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // 🔥 bigger height
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // ── LEFT: Logo + Title ─────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Logo
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val isPreview = LocalInspectionMode.current

                    if (!isPreview) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Mazra3ty Logo",
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        // 👇 بديل بسيط للـ Preview
                        Text(
                            text = "🌱",
                            fontSize = 20.sp
                        )
                    }
                }

                // Animated Title
                AnimatedContent(
                    targetState = pageTitle,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "topbar_title"
                ) { title ->
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                }
            }

            // ── RIGHT: Logout Button ─────────────────────────────
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(RedError.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.ExitToApp,
                    contentDescription = "Logout",
                    tint = RedError,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── Bottom Accent Line ─────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(GreenPrimary.copy(alpha = 0.3f))
        )
    }
}
@Preview(showBackground = true)
@Composable
fun AdminTopBarPreview() {
    Mazra3tyTheme {
        AdminTopBar(
            currentRoute = AdminScreen.Dashboard.route,
            onLogout = {}
        )
    }
}