package com.mazra3ty.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.types.UserWithProfile
import com.mazra3ty.app.ui.theme.GrayDark
import com.mazra3ty.app.ui.theme.GrayLight
import com.mazra3ty.app.ui.theme.GrayMedium
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.RedError
import com.mazra3ty.app.ui.theme.TextPrimary
// ─── Top Bar ─────────────────────────────────────────────────────────────────
@Composable
fun HomeTopBar(
    user: UserWithProfile,
    searchQuery: String,
    unreadCount: Int = 0,
    onSearchChange: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF0F7E8))
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // ── Profile row ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary.copy(alpha = 0.15f))
                    .border(2.dp, GreenPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name + rating + phone
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.full_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    // Role badge
                    RoleBadge(role = user.role)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("4.5", fontSize = 12.sp, color = Color(0xFFFFC107), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(10.dp))
                    user.phone?.let {
                        Icon(
                            Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = GrayMedium,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(it, fontSize = 12.sp, color = GrayMedium)
                    }
                }
            }

            // Notification bell
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F7E8))
                    .border(1.dp, GreenPrimary.copy(alpha = 0.3f), CircleShape)
                    .clickable { onNotificationsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = GreenPrimary
                )

                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(RedError)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

        }

        Spacer(Modifier.height(14.dp))

        // ── Search bar ────────────────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search workers, jobs...", color = GrayMedium, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, null, tint = GrayMedium, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Outlined.Close, null, tint = GrayMedium, modifier = Modifier.size(18.dp))
                    }
                }
            },
            shape = RoundedCornerShape(32.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}
// ─── Role Badge ───────────────────────────────────────────────────────────────

@Composable
private fun RoleBadge(role: String) {
    val (bg, tint, label) = when (role) {
        "farmer" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Farmer")
        "worker" -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "Worker")
        "admin"  -> Triple(Color(0xFFFCE4EC), Color(0xFFC62828), "Admin")
        else     -> Triple(GrayLight, GrayDark, role)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 10.sp, color = tint, fontWeight = FontWeight.SemiBold)
    }
}
