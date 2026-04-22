package com.mazra3ty.app.ui.component
import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mazra3ty.app.ui.theme.GreenPrimary
import com.mazra3ty.app.ui.theme.RedError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DEFAULT_LOCATION = LatLng(36.19, 5.41)

@Composable
fun MapPickerDialog(
    initialAddress: String = "",
    onLocationPicked: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        MapPickerContent(
            initialAddress = initialAddress,
            onLocationPicked = onLocationPicked,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapPickerContent(
    initialAddress: String,
    onLocationPicked: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── State ─────────────────────────────────────────────────────────────────
    var searchQuery   by remember { mutableStateOf(initialAddress) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf("") }
    var isSearching   by remember { mutableStateOf(false) }
    var searchError   by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, 6f)
    }

    // ── Reverse geocode a tap on the map ──────────────────────────────────────
    fun reverseGeocode(latLng: LatLng) {
        scope.launch {
            isSearching = true
            searchError = null
            try {
                val geocoder = Geocoder(context)
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                }
                if (!results.isNullOrEmpty()) {
                    val addr = results[0]
                    selectedAddress = buildString {
                        append(addr.locality ?: addr.subAdminArea ?: "")
                        if (addr.adminArea != null) append(", ${addr.adminArea}")
                        if (addr.countryName != null) append(", ${addr.countryName}")
                    }.trim().trimStart(',').trim()
                    searchQuery = selectedAddress
                } else {
                    selectedAddress = "${latLng.latitude.format(4)}, ${latLng.longitude.format(4)}"
                    searchQuery = selectedAddress
                }
            } catch (e: Exception) {
                selectedAddress = "${latLng.latitude.format(4)}, ${latLng.longitude.format(4)}"
                searchQuery = selectedAddress
            } finally {
                isSearching = false
            }
        }
    }

    // ── Forward geocode a typed search ────────────────────────────────────────
    fun searchAddress(query: String) {
        if (query.isBlank()) return
        scope.launch {
            isSearching = true
            searchError = null
            try {
                val geocoder = Geocoder(context)
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 1)
                }
                if (!results.isNullOrEmpty()) {
                    val addr = results[0]
                    val latLng = LatLng(addr.latitude, addr.longitude)
                    selectedLatLng = latLng
                    selectedAddress = buildString {
                        append(addr.locality ?: addr.subAdminArea ?: query)
                        if (addr.adminArea != null) append(", ${addr.adminArea}")
                        if (addr.countryName != null) append(", ${addr.countryName}")
                    }.trim().trimStart(',').trim()
                    searchQuery = selectedAddress
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 13f)
                    )
                } else {
                    searchError = "Location not found. Try a different search."
                }
            } catch (e: Exception) {
                searchError = "Search failed. Check your connection."
            } finally {
                isSearching = false
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.DarkGray)
                }
                Text(
                    "Pick Location",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                // Confirm button — enabled only when a spot is picked
                AnimatedVisibility(visible = selectedLatLng != null) {
                    Button(
                        onClick = {
                            selectedLatLng?.let { ll ->
                                onLocationPicked(selectedAddress, ll.latitude, ll.longitude)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; searchError = null },
                    placeholder = { Text("Search city or address…", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = {
                        if (isSearching)
                            CircularProgressIndicator(
                                color = GreenPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        else
                            Icon(Icons.Outlined.Search, null, tint = GreenPrimary)
                    },
                    trailingIcon = {
                        AnimatedVisibility(searchQuery.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                            IconButton(onClick = { searchQuery = ""; searchError = null }) {
                                Icon(Icons.Outlined.Close, null, tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedContainerColor = Color(0xFFEEF7E8),
                        unfocusedContainerColor = Color(0xFFF8F8F8)
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { searchAddress(searchQuery) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Search error
                AnimatedVisibility(searchError != null) {
                    Text(
                        searchError ?: "",
                        color = RedError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                // Hint
                if (selectedLatLng == null) {
                    Text(
                        "Tap the map or search to pick a location",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // ── Map ───────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {

                // ── Map ─────────────────────────────────────────
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false
                    ),
                    onMapClick = { latLng ->
                        selectedLatLng = latLng
                        reverseGeocode(latLng)
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(latLng, 14f)
                            )
                        }
                    }
                ) {
                    selectedLatLng?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = selectedAddress.ifBlank { "Selected location" },
                            snippet = "${latLng.latitude.format(5)}, ${latLng.longitude.format(5)}"
                        )
                    }
                }

                // ── Bottom Chip ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)

                ) {

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = false,
                            mapToolbarEnabled = false
                        ),
                        onMapClick = { latLng ->
                            selectedLatLng = latLng
                            reverseGeocode(latLng)
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(latLng, 14f)
                                )
                            }
                        }
                    ) {
                        selectedLatLng?.let { latLng ->
                            Marker(
                                state = MarkerState(position = latLng),
                                title = selectedAddress.ifBlank { "Selected location" },
                                snippet = "${latLng.latitude.format(5)}, ${latLng.longitude.format(5)}"
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedLatLng != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null)
                                Text(selectedAddress)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Extension ─────────────────────────────────────────────────────────────────

private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)