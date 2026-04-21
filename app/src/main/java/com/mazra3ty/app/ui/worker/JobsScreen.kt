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

@Composable
fun JobsScreen(
    userEmail: String = "",
    userName: String = ""
) {
    // ===== States =====
    var searchQuery  by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf("All") }
    var selectedJob  by remember { mutableStateOf<Job?>(null) } // ← للتفاصيل

    val chips = listOf("All", "Recent", "Palmes", "Trees", "Explore")

    // ===== بيانات الوظائف =====
    val allJobs = listOf(
        Job(
            id           = "1",
            title        = "Tomato Picker",
            price        = "3000 da",
            duration     = "3 Days",
            location     = "Ghardaia • 3 km",
            category     = "All",
            description  = "We need experienced tomato pickers for our farm. Work starts early morning and ends at noon.",
            farmerName   = "Ahmed Benali",
            farmerEmail  = "ahmed@farm.dz",
            requirements = "- Physical fitness\n- Experience in harvesting\n- Available for 3 days",
            postedDate   = "2 days ago"
        ),
        Job(
            id           = "2",
            title        = "Palm Harvester",
            price        = "2500 da",
            duration     = "5 Days",
            location     = "Ghardaia • 5 km",
            category     = "Palmes",
            description  = "Harvest dates from palm trees. Tools provided by the farm.",
            farmerName   = "Karim Ouled",
            farmerEmail  = "karim@palms.dz",
            requirements = "- Not afraid of heights\n- Good physical condition",
            postedDate   = "1 day ago"
        ),
        Job(
            id           = "3",
            title        = "Tree Planter",
            price        = "2000 da",
            duration     = "2 Days",
            location     = "Ghardaia • 1 km",
            category     = "Trees",
            description  = "Plant young olive trees in our new field. Training provided.",
            farmerName   = "Youcef Hamdi",
            farmerEmail  = "youcef@olive.dz",
            requirements = "- No experience needed\n- Must be punctual",
            postedDate   = "Today"
        )
    )

    // ===== فلترة الوظائف =====
    val filteredJobs = allJobs.filter { job ->
        val matchChip   = selectedChip == "All" || job.category == selectedChip
        val matchSearch = job.title.contains(searchQuery, ignoreCase = true)
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

            LazyColumn(
                modifier       = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredJobs) { job ->
                    JobCard(
                        job          = job,
                        onApplyClick = { selectedJob = it } // ← يفتح التفاصيل
                    )
                }
            }
        }
    }

    // ===== Bottom Sheet التفاصيل =====
    // يظهر فقط عند الضغط على Apply Now
    selectedJob?.let { job ->
        JobDetailSheet(
            job         = job,
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