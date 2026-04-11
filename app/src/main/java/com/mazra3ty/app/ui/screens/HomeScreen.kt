package com.mazra3ty.app.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mazra3ty.app.ui.theme.*

@Composable
fun HomeScreen(
    userEmail: String,
    onLogout: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SoftGreenBg, Color(0xFFEAF3DA))
                )
            )
            .padding(20.dp)
    ) {

        // 👋 Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null,
                tint = GreenPrimary
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Welcome 👋",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = userEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(30.dp))

        // 🧑 Profile card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null)

                Spacer(Modifier.width(12.dp))

                Column {
                    Text("Your Profile")
                    Text(
                        "Manage your account",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 🚪 Logout
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }

}
