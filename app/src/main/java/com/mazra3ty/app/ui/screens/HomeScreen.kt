package com.mazra3ty.app.ui.screens

import androidx.compose.runtime.Composable
import com.mazra3ty.app.navigation.AppNavHost

/**
 * Thin wrapper so AuthHost can call HomeScreen without knowing about navigation internals.
 * NOTE: userEmail is intentionally absent — email lives in auth.users, not public.users.
 */
@Composable
fun HomeScreen(
    userId:   String,
    userRole: String,
    onLogout: () -> Unit
) {
    AppNavHost(
        userId   = userId,
        userRole = userRole,
        onLogout = onLogout
    )
}