package com.mazra3ty.app.ui.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.database.SupabaseClientProvider
import com.mazra3ty.app.database.types.WorkerPost
import com.mazra3ty.app.database.types.WorkerPostStatus
import com.mazra3ty.app.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerMyPostsScreen(
    workerId: String,
    onBack: () -> Unit,
    onCreatePost: () -> Unit
) {
    var posts     by remember { mutableStateOf<List<WorkerPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error     by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            posts = SupabaseClientProvider.client
                .postgrest["worker_posts"]
                .select { filter { eq("worker_id", workerId) } }
                .decodeList<WorkerPost>()
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAF5),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Posts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${posts.size} published", fontSize = 12.sp, color = Color(0xFF888888))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ArrowBack, null, tint = GreenPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onCreatePost,
                containerColor = GreenPrimary,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("New Post", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = GreenPrimary) }

            posts.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📝", fontSize = 48.sp)
                    Text("No posts yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Create your first post so farmers\ncan find you",
                        fontSize  = 13.sp,
                        color     = Color(0xFF888888),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(60.dp))
                }
            }

            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(
                    start  = 16.dp, end = 16.dp,
                    top    = 12.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts) { post ->
                    WorkerPostCard(post = post)
                }
            }
        }
    }
}

@Composable
private fun WorkerPostCard(post: WorkerPost) {
    val isActive = post.status == WorkerPostStatus.active

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    post.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = Color(0xFF1A1A1A),
                    modifier   = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) GreenPrimary.copy(alpha = 0.12f)
                    else Color(0xFFEEEEEE)
                ) {
                    Text(
                        if (isActive) "Active" else "Inactive",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize   = 11.sp,
                        color      = if (isActive) GreenPrimary else Color(0xFF757575),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                post.description.take(100) + if (post.description.length > 100) "..." else "",
                fontSize   = 13.sp,
                color      = Color(0xFF555555),
                lineHeight = 18.sp
            )

            if (post.skills.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.skills.take(3).forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GreenPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                skill,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                color    = GreenPrimary
                            )
                        }
                    }
                }
            }

            post.location?.let {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(8.dp))
                Text("📍 $it", fontSize = 12.sp, color = Color(0xFF888888))
            }

            post.created_at?.take(10)?.let {
                Text("Posted: $it", fontSize = 11.sp, color = Color(0xFFAAAAAA))
            }
        }
    }
}