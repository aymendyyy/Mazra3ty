package com.mazra3ty.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.User
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.RedError
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

// ─── States ───────────────────────────────────────────────────────────────────

private sealed class AdminAccessState {
    object Loading : AdminAccessState()
    object Authorized : AdminAccessState()
    object Unauthorized : AdminAccessState()
    object NotLoggedIn : AdminAccessState()
}

// ─── Guard ────────────────────────────────────────────────────────────────────

/**
 * Wrap any admin screen with this composable.
 * It checks the current logged-in user's role from Supabase.
 * Only role == "admin" can pass through.
 *
 * Usage:
 *   AdminGuard(onUnauthorized = { navController.popBackStack() }) {
 *       AdminDashboardScreen(...)
 *   }
 */
@Composable
fun AdminGuard(
    onUnauthorized: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<AdminAccessState>(AdminAccessState.Loading) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // 1. Get current session user id
                val session = SupabaseClientProvider.client.auth.currentSessionOrNull()
                if (session == null) {
                    state = AdminAccessState.NotLoggedIn
                    return@launch
                }

                val uid = session.user?.id
                if (uid == null) {
                    state = AdminAccessState.NotLoggedIn
                    return@launch
                }

                // 2. Fetch user from our users table
                val user = SupabaseClientProvider.client
                    .postgrest["users"]
                    .select {
                        filter { eq("id", uid) }
                    }
                    .decodeSingle<User>()

                // 3. Check role
                state = if (user.role == "admin") {
                    AdminAccessState.Authorized
                } else {
                    AdminAccessState.Unauthorized
                }

            } catch (e: Exception) {
                e.printStackTrace()
                state = AdminAccessState.Unauthorized
            }
        }
    }

    when (state) {
        AdminAccessState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }

        AdminAccessState.Authorized -> {
            content()
        }

        AdminAccessState.Unauthorized,
        AdminAccessState.NotLoggedIn -> {
            AccessDeniedScreen(
                isLoggedIn = state != AdminAccessState.NotLoggedIn,
                onGoBack = onUnauthorized
            )
        }
    }
}

// ─── Access Denied UI ─────────────────────────────────────────────────────────

@Composable
private fun AccessDeniedScreen(isLoggedIn: Boolean, onGoBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = RedError,
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isLoggedIn) "Access Denied" else "Not Logged In",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = RedError
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isLoggedIn)
                    "You don't have permission to access this page.\nAdmin role required."
                else
                    "Please log in to continue.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onGoBack,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Go Back", color = Color.White)
            }
        }
    }
}