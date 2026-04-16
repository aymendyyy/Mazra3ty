package com.mazra3ty.app.ui.component
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.GreenPrimaryDark
import com.mazra3ty.app.ui.theme.Mazra3tyTheme

// ─── Route constants ──────────────────────────────────────────────────────────

object WorkerRoutes {
    const val HOME        = "worker_home"
    const val JOBS        = "worker_jobs"
    const val POSTS       = "worker_posts"
    const val MESSAGES    = "worker_messages"
    const val PROFILE     = "worker_profile"
}

object FarmerRoutes {
    const val HOME        = "farmer_home"
    const val MY_JOBS     = "farmer_my_jobs"
    const val MESSAGES    = "farmer_messages"
    const val WORKER_FEED = "farmer_worker_feed"
    const val PROFILE     = "farmer_profile"
}

// ─── Nav item model ───────────────────────────────────────────────────────────

data class NavItem(
    val route:       String,
    val iconFilled:  ImageVector,
    val iconOutlined: ImageVector
)

// ─── Worker nav items ─────────────────────────────────────────────────────────

val workerNavItems = listOf(
    NavItem(
        route        = WorkerRoutes.HOME,
        iconFilled   = Icons.Filled.Home,
        iconOutlined = Icons.Outlined.Home
    ),
    NavItem(
        route        = WorkerRoutes.JOBS,
        iconFilled   = Icons.Filled.Work,
        iconOutlined = Icons.Outlined.Work
    ),
    NavItem(
        route        = WorkerRoutes.POSTS,
        iconFilled   = Icons.Filled.Article,
        iconOutlined = Icons.Outlined.Article
    ),
    NavItem(
        route        = WorkerRoutes.MESSAGES,
        iconFilled   = Icons.Filled.Chat,
        iconOutlined = Icons.Outlined.Chat
    ),
    NavItem(
        route        = WorkerRoutes.PROFILE,
        iconFilled   = Icons.Filled.Person,
        iconOutlined = Icons.Outlined.Person
    )
)

// ─── Farmer nav items ─────────────────────────────────────────────────────────

val farmerNavItems = listOf(
    NavItem(
        route        = FarmerRoutes.HOME,
        iconFilled   = Icons.Filled.Home,
        iconOutlined = Icons.Outlined.Home
    ),
    NavItem(
        route        = FarmerRoutes.MY_JOBS,
        iconFilled   = Icons.Filled.Inventory2,
        iconOutlined = Icons.Outlined.Inventory2
    ),
    NavItem(
        route        = FarmerRoutes.WORKER_FEED,
        iconFilled   = Icons.Filled.People,
        iconOutlined = Icons.Outlined.People
    ),
    NavItem(
        route        = FarmerRoutes.MESSAGES,
        iconFilled   = Icons.Filled.Chat,
        iconOutlined = Icons.Outlined.Chat
    ),
    NavItem(
        route        = FarmerRoutes.PROFILE,
        iconFilled   = Icons.Filled.Person,
        iconOutlined = Icons.Outlined.Person
    )
)

// ─── Shared Bottom Bar ────────────────────────────────────────────────────────

/**
 * Generic animated bottom nav bar used by both roles.
 *
 * @param items        List of [NavItem] for this role
 * @param currentRoute Active route string
 * @param onNavigate   Callback with the tapped route
 * @param unreadCounts Map of route → unread badge count (optional)
 */
@Composable
fun AppBottomBar(
    items:        List<NavItem>,
    currentRoute: String,
    onNavigate:   (String) -> Unit,
    unreadCounts: Map<String, Int> = emptyMap()
) {
    Surface(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        shape     = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color     = Color.White,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavBarItem(
                    item         = item,
                    selected     = selected,
                    badgeCount   = unreadCounts[item.route] ?: 0,
                    onClick      = { if (!selected) onNavigate(item.route) },
                    modifier     = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── Single Nav Item ──────────────────────────────────────────────────────────

@Composable
private fun NavBarItem(
    item:       NavItem,
    selected:   Boolean,
    badgeCount: Int,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) GreenPrimary else Color(0xFFAAAAAA),
        animationSpec = tween(250),
        label = "iconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) GreenPrimaryDark else Color(0xFFAAAAAA),
        animationSpec = tween(250),
        label = "labelColor"
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 48.dp else 0.dp,
        animationSpec = tween(300),
        label = "pillWidth"
    )

    IconButton(
        onClick  = onClick,
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Pill indicator + icon
            Box(contentAlignment = Alignment.Center) {
                // Animated pill background
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(pillWidth)
                        .clip(RoundedCornerShape(50))
                        .background(GreenPrimary.copy(alpha = 0.12f))
                )

                // Icon with optional badge
                BadgedBox(
                    badge = {
                        if (badgeCount > 0) {
                            Badge(
                                containerColor = Color(0xFFEF4444),
                                contentColor   = Color.White
                            ) {
                                Text(
                                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector     = if (selected) item.iconFilled else item.iconOutlined,
                        contentDescription="",
                        tint            = iconColor,
                        modifier        = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(3.dp))
        }
    }
}

// ─── Role-specific wrappers ───────────────────────────────────────────────────

/**
 * Bottom bar for Workers.
 * Tabs: Home · Jobs · My Posts · Messages · Profile
 */
@Composable
fun WorkerBottomBar(
    currentRoute: String,
    onNavigate:   (String) -> Unit,
    unreadMessages: Int = 0
) {
    AppBottomBar(
        items        = workerNavItems,
        currentRoute = currentRoute,
        onNavigate   = onNavigate,
        unreadCounts = mapOf(WorkerRoutes.MESSAGES to unreadMessages)
    )
}

/**
 * Bottom bar for Farmers.
 * Tabs: Home · My Jobs · Workers · Messages · Profile
 */
@Composable
fun FarmerBottomBar(
    currentRoute: String,
    onNavigate:   (String) -> Unit,
    unreadMessages: Int = 0
) {
    AppBottomBar(
        items        = farmerNavItems,
        currentRoute = currentRoute,
        onNavigate   = onNavigate,
        unreadCounts = mapOf(FarmerRoutes.MESSAGES to unreadMessages)
    )
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun WorkerBottomBarPreview() {
    Mazra3tyTheme {
        Column {
            Spacer(Modifier.height(40.dp))
            WorkerBottomBar(
                currentRoute    = WorkerRoutes.JOBS,
                onNavigate      = {},
                unreadMessages  = 3
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun FarmerBottomBarPreview() {
    Mazra3tyTheme {
        Column {
            Spacer(Modifier.height(40.dp))
            FarmerBottomBar(
                currentRoute    = FarmerRoutes.HOME,
                onNavigate      = {},
                unreadMessages  = 12
            )
        }
    }
}