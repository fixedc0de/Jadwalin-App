package com.oxygen.jadwalin.ui.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oxygen.jadwalin.data.local.JadwalEntity
import com.oxygen.jadwalin.ui.viewmodel.JadwalUiState
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JadwalScreen(
    vm: JadwalViewModel,
    onEditJadwal: (JadwalEntity) -> Unit = {}
) {
    val uiState by vm.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var selectedSortOption by remember { mutableStateOf("terdekat") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<JadwalEntity?>(null) }

    val colors = JadwalinTheme.colors
    val context = LocalContext.current

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Urutkan Jadwal", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                val sortOptions = listOf(
                    "terdekat" to "Urutkan dari terdekat",
                    "terjauh" to "Urutkan dari Terjauh",
                    "a-z" to "Urutkan A-Z"
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pilih Urutan Tampilan:", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    sortOptions.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSortOption = key
                                    showFilterDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedSortOption == key, onClick = {
                                selectedSortOption = key
                                showFilterDialog = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Tutup", color = colors.primary)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Konfirmasi Hapus", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text("Apakah yakin ingin menghapus jadwal ${item.judul}?", color = colors.textPrimary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val judulText = item.judul
                        vm.hapusJadwal(item)
                        itemToDelete = null
                        Toast.makeText(context, "Jadwal '$judulText' berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Hapus", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal", color = colors.primary)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    Scaffold(
        containerColor = colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Jadwal",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                        Row {
                            IconButton(onClick = { 
                                vm.syncData() 
                                Toast.makeText(context, "Sinkronisasi cloud sedang berjalan...", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Rounded.Sync, null, tint = colors.primary)
                            }
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(Icons.Rounded.FilterList, null, tint = colors.textPrimary)
                            }
                        }
                    }
                    Text(
                        text = "Kelola agenda harian Anda di sini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Cari judul jadwal...", color = colors.textHint, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.textHint) },
                    shape = JadwalinShapes.Input,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.cardBackground,
                        unfocusedContainerColor = colors.cardBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }

            item {
                val filters = listOf("Semua", "Kuliah", "Sekolah", "Kerja")
                LazyRow(
                    modifier = Modifier.fillMaxWidth().offset(x = (-24).dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            onClick = { selectedFilter = filter },
                            color = if (isSelected) colors.primaryDark else colors.cardBackground,
                            shape = JadwalinShapes.Chip,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
                        ) {
                            Text(
                                text = filter,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                JadwalStatusBar(
                    onActivateAll = {
                        if (uiState is JadwalUiState.Success) {
                            val list = (uiState as JadwalUiState.Success).data
                            if (list.isNotEmpty()) {
                                list.forEach { item ->
                                    vm.updateJadwal(item.copy(is_active = true))
                                }
                                Toast.makeText(context, "Semua jadwal berhasil diaktifkan", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Tidak ada jadwal untuk diaktifkan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            when (val state = uiState) {
                is JadwalUiState.Loading -> {
                    item { Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { CircularProgressIndicator(color = colors.primary) } }
                }
                is JadwalUiState.Success -> {
                    val filtered = state.data.filter { 
                        (selectedFilter == "Semua" || it.kategori.equals(selectedFilter, ignoreCase = true)) &&
                        (it.judul.contains(searchQuery, ignoreCase = true))
                    }.let { list ->
                        when (selectedSortOption) {
                            "terjauh" -> list.sortedByDescending { it.waktu_mulai }
                            "a-z" -> list.sortedBy { it.judul.lowercase() }
                            else -> list.sortedBy { it.waktu_mulai }
                        }
                    }

                    if (filtered.isEmpty()) item { EmptyJadwalView() }
                    else items(filtered) { item ->
                        DetailedAlarmCard(
                            item = item,
                            onToggle = { 
                                vm.updateJadwal(item.copy(is_active = !item.is_active))
                            },
                            onDelete = { 
                                itemToDelete = item
                            },
                            onEdit = {
                                onEditJadwal(item)
                            }
                        ) 
                    }
                }
                is JadwalUiState.Error -> { item { Text("Error: ${state.msg}", color = colors.error) } }
            }
            
            item { Spacer(modifier = Modifier.height(140.dp)) }
        }
    }
}

@Composable
fun JadwalStatusBar(onActivateAll: () -> Unit) {
    val colors = JadwalinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(colors.success))
            Spacer(Modifier.width(8.dp))
            Text("Sistem Siap", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        }
        Text(
            text = "Aktifkan Semua",
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onActivateAll() }
        )
    }
}

@Composable
fun DetailedAlarmCard(
    item: JadwalEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val colors = JadwalinTheme.colors
    val time = try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(item.waktu_mulai)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date!!)
    } catch (e: Exception) { "00:00" }

    val categoryColor = when (item.kategori.lowercase()) {
        "kuliah" -> colors.catKuliah
        "kerja", "proyek" -> colors.catKerja
        "pribadi" -> colors.catPribadi
        else -> colors.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (item.is_active) 1f else 0.7f).clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = JadwalinShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
    ) {
        Row(Modifier.fillMaxWidth().height(intrinsicSize = IntrinsicSize.Min)) {
            Box(Modifier.fillMaxHeight().width(6.dp).background(categoryColor))
            Column(Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(time, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                        Text(" WIB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textHint, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = categoryColor.copy(alpha = 0.15f), shape = JadwalinShapes.Chip) {
                            Text(item.kategori.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = categoryColor)
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, null, tint = colors.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Text(item.judul, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Repeat, null, tint = colors.textHint, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (item.isRecurring) "Berulang" else "Sekali", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    if (item.lokasi != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "●", fontSize = 6.sp, color = colors.divider)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Rounded.Room, null, tint = colors.textHint, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(item.lokasi, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = if (item.is_active) colors.success.copy(alpha = 0.15f) else colors.pillBackground, shape = CircleShape) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (item.is_active) Icons.Rounded.Schedule else Icons.Rounded.AlarmOff, null, tint = if (item.is_active) colors.success else colors.textHint, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (item.is_active) "Aktif" else "Nonaktif", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (item.is_active) colors.success else colors.textHint)
                        }
                    }
                    Switch(
                        checked = item.is_active, 
                        onCheckedChange = { onToggle() }, 
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.primary)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyJadwalView() {
    val colors = JadwalinTheme.colors
    Box(Modifier.fillMaxWidth().padding(top = 80.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.SearchOff, null, tint = colors.textHint.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Jadwal tidak ditemukan", color = colors.textHint, fontWeight = FontWeight.Bold)
        }
    }
}
