package com.mazra3ty.app.ui.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazra3ty.app.ui.theme.*
import androidx.compose.runtime.LaunchedEffect
import com.mazra3ty.app.database.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

@Composable
fun JobsScreen(
    userId: String = "",
    userEmail: String = "",
    userName: String = "",
    onViewApplications: () -> Unit = {},
    onViewMyPosts: () -> Unit = {}
) {
    // ===== States =====
    var searchQuery  by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf("All") }
    var selectedJob  by remember { mutableStateOf<Job?>(null) } // ← للتفاصيل

    val chips = listOf("All", "Recent", "Palmes", "Trees", "Explore")

    // ===== جلب البيانات من Supabase =====
    var allJobs   by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error     by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            // 1. جلب الوظائف من جدول jobs
            val dbJobs = SupabaseClientProvider.client
                .postgrest["jobs"]
                .select()
                .decodeList<com.mazra3ty.app.database.types.Job>()

            // 2. جلب أسماء المزارعين من جدول users
            val farmerIds = dbJobs.map { it.farmer_id }.distinct()
            val farmers = if (farmerIds.isNotEmpty()) {
                SupabaseClientProvider.client
                    .postgrest["users"]
                    .select {
                        filter {
                            isIn("id", farmerIds)
                        }
                    }
                    .decodeList<com.mazra3ty.app.database.types.User>()
            } else emptyList()

            val farmerMap = farmers.associateBy { it.id }

            // 3. تحويل البيانات إلى Job الخاص بالواجهة
            allJobs = dbJobs.map { dbJob ->
                val farmer = farmerMap[dbJob.farmer_id]
                Job(
                    id          = dbJob.id,
                    title       = dbJob.title,
                    price       = if (dbJob.salary != null) "${dbJob.salary.toInt()} DA" else "Negotiable",
                    duration    = "",
                    location    = dbJob.location ?: "",
                    category    = "All",
                    description = dbJob.description,
                    farmerName  = farmer?.full_name ?: "Farm Owner",
                    farmerEmail = farmer?.email ?: "",
                    requirements = "",
                    postedDate  = dbJob.created_at?.take(10) ?: "",
                    farmerId    = dbJob.farmer_id
                )
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    // ===== فلترة الوظائف =====
    val filteredJobs = allJobs.filter { job ->
        val matchChip   = selectedChip == "All" || job.category == selectedChip
        val matchSearch = searchQuery.isBlank() ||
                job.title.contains(searchQuery.trim(), ignoreCase = true) ||
                job.location.contains(searchQuery.trim(), ignoreCase = true) ||
                job.farmerName.contains(searchQuery.trim(), ignoreCase = true)
        matchChip && matchSearch
    }

    // ===== UI =====
    Scaffold(
        containerColor = Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TopBar()

            SearchBar(
                query         = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            FilterChips(
                chips          = chips,
                selectedChip   = selectedChip,
                onChipSelected = { selectedChip = it }
            )

            when {
                isLoading -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                }
                error != null -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Failed to load jobs\n${error}",
                            color     = TextGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                filteredJobs.isEmpty() -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No jobs found", color = TextGray)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier       = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredJobs) { job ->
                            JobCard(
                                job          = job,
                                onApplyClick = { selectedJob = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // ===== Bottom Sheet التفاصيل =====
    // يظهر فقط عند الضغط على Apply Now
    selectedJob?.let { job ->
        JobDetailSheet(
            job         = job,
            workerId    = userId,
            workerEmail = userEmail,
            workerName  = userName,
            onDismiss   = { selectedJob = null },
            onApplied   = { selectedJob = null }
        )
    }
}

// ===== TOP BAR =====
@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = Icons.Default.Eco,
                contentDescription = "Logo",
                tint               = GreenPrimary,
                modifier           = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text       = "Mazra3ty",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextDark
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GreenLight)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint               = GreenPrimary
            )
        }
    }
}

// ===== SEARCH BAR =====
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── حقل البحث ──
        OutlinedTextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier
                .weight(1f)
                .height(52.dp),
            placeholder   = {
                Text(
                    "Search jobs...",
                    color = TextGray,
                    fontSize = 14.sp
                )
            },
            leadingIcon   = {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = "Search",
                    tint               = if (query.isNotBlank()) GreenPrimary else TextGray,
                    modifier           = Modifier.size(20.dp)
                )
            },
            // ← زر X لمسح البحث
            trailingIcon  = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint               = TextGray,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine    = true,
            shape         = RoundedCornerShape(24.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = GreenLight,
                unfocusedContainerColor = GreenLight,
                focusedBorderColor      = GreenPrimary,
                unfocusedBorderColor    = androidx.compose.ui.graphics.Color.Transparent,
                focusedTextColor        = TextDark,
                unfocusedTextColor      = TextDark,
                cursorColor             = GreenPrimary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // ── زر الفلتر ──
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GreenLight)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Tune,
                contentDescription = "Filter",
                tint               = GreenPrimary
            )
        }
    }
}
// ===== FILTER CHIPS =====
@Composable
fun FilterChips(
    chips: List<String>,
    selectedChip: String,
    onChipSelected: (String) -> Unit
) {
    LazyRow(
        modifier       = Modifier.padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip ->
            val isSelected = chip == selectedChip
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) GreenPrimary else White)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = CardBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onChipSelected(chip) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = chip,
                    fontSize = 13.sp,
                    color    = if (isSelected) White else TextDark
                )
            }
        }
    }
}