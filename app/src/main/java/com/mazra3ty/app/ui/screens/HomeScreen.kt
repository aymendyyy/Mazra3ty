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
import com.mazra3ty.app.ui.theme.*
import com.mazra3ty.app.ui.admin.*

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — role-aware entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    userEmail: String,
    userRole:  String,
    onLogout:  () -> Unit
) {
    when (userRole) {
        "admin"  -> AdminHome(userEmail = userEmail, onLogout = onLogout)
        "farmer" -> FarmerHome(userEmail = userEmail, onLogout = onLogout)
        else     -> WorkerHome(userEmail = userEmail, onLogout = onLogout)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Admin layout
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────
// Farmer / Worker placeholders
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FarmerHome(userEmail: String, onLogout: () -> Unit) {
    RolePlaceholderScreen(role = "Farmer 🌾", email = userEmail, onLogout = onLogout)
}

@Composable
private fun WorkerHome(userEmail: String, onLogout: () -> Unit) {
    RolePlaceholderScreen(role = "Worker 👷", email = userEmail, onLogout = onLogout)
}

@Composable
private fun RolePlaceholderScreen(role: String, email: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SoftGreenBg, Color(0xFFEAF3DA))))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = GreenPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Welcome $role", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))
        Text(email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(30.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Your Profile")
                    Text("Manage your account", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick  = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Icon(Icons.Outlined.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}