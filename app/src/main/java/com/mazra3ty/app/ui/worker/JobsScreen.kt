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
fun JobsScreen() {

    // ===== State =====
    var searchQuery     by remember { mutableStateOf("") }
    var selectedChip    by remember { mutableStateOf("All") }
    var selectedNav     by remember { mutableStateOf("Jobs") }

    val chips = listOf("All", "Recent", "Palmes", "Trees", "Explore")

    val allJobs = listOf(
        Job("Tomato Picker", "3000 da", "3 Days", "Ghardaia • 3 km", "All"),
        Job("Palm Harvester", "2500 da", "5 Days", "Ghardaia • 5 km", "Palmes"),
        Job("Tree Planter",   "2000 da", "2 Days", "Ghardaia • 1 km", "Trees"),
    )

    // فلترة القائمة
    val filteredJobs = allJobs.filter { job ->
        val matchChip  = selectedChip == "All" || job.category == selectedChip
        val matchSearch = job.title.contains(searchQuery, ignoreCase = true)
        matchChip && matchSearch
    }

    // ===== UI =====
    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNavigationBar(
                selectedNav = selectedNav,
                onNavSelected = { selectedNav = it }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ===== TOP BAR =====
            TopBar()

            // ===== SEARCH BAR =====
            SearchBar(
                query    = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            // ===== FILTER CHIPS =====
            FilterChips(
                chips       = chips,
                selectedChip = selectedChip,
                onChipSelected = { selectedChip = it }
            )

            // ===== JOB LIST =====
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredJobs) { job ->
                    JobCard(
                        job          = job,
                        onApplyClick = { /* TODO */ }
                    )
                }
            }
        }
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
                imageVector = Icons.Default.Eco,
                contentDescription = "Logo",
                tint     = GreenPrimary,
                modifier = Modifier.size(32.dp)
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
        // Search Field
        Card(
            modifier  = Modifier.weight(1f).height(48.dp),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = GreenLight),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = "Search",
                    tint               = TextGray,
                    modifier           = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    placeholder   = { Text("Search..", color = TextGray) },
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = GreenLight,
                        unfocusedContainerColor = GreenLight,
                        focusedIndicatorColor   = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Filter Button
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

// ===== BOTTOM NAV =====
@Composable
fun BottomNavigationBar(
    selectedNav: String,
    onNavSelected: (String) -> Unit
) {
    val items = listOf(
        Pair("Home",    Icons.Default.Home),
        Pair("Jobs",    Icons.Default.Work),
        Pair("Chat",    Icons.Default.Chat),
        Pair("Profile", Icons.Default.Person)
    )

    NavigationBar(containerColor = White) {
        items.forEach { (label, icon) ->
            NavigationBarItem(
                selected = selectedNav == label,
                onClick  = { onNavSelected(label) },
                icon     = {
                    Icon(
                        imageVector        = icon,
                        contentDescription = label
                    )
                },
                label  = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = GreenPrimary,
                    selectedTextColor   = GreenPrimary,
                    unselectedIconColor = TextGray,
                    unselectedTextColor = TextGray,
                    indicatorColor      = GreenLight
                )
            )
        }
    }
}