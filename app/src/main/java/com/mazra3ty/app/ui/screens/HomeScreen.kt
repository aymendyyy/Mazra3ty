package com.mazra3ty.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mazra3ty.app.navigation.AppNavHost
import com.mazra3ty.app.ui.theme.*
import com.mazra3ty.app.ui.admin.*

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — role-aware entry point
// Call site: HomeScreen(userEmail, userRole, onLogout)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    userId: String,
    userEmail: String,
    userRole: String,
    onLogout: () -> Unit
) {
    AppNavHost(
        userId = userId,
        userEmail = userEmail,
        userRole = userRole,
        onLogout = onLogout
    )
}