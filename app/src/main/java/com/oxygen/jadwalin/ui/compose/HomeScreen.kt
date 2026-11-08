package com.oxygen.jadwalin.ui.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oxygen.jadwalin.data.local.JadwalEntity
import com.oxygen.jadwalin.ui.viewmodel.AuthViewModel
import com.oxygen.jadwalin.ui.viewmodel.JadwalUiState
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import com.oxygen.jadwalin.ui.viewmodel.SettingsViewModel
import com.oxygen.jadwalin.util.AlarmScheduler
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    vm: JadwalViewModel? = null,
    authVm: AuthViewModel? = null,
    onNavigateToJadwal: () -> Unit = {}
) {
    val uiState by if (vm != null) vm.uiState.collectAsState() else remember { mutableStateOf(JadwalUiState.Loading) }
    val userName by if (authVm != null) authVm.userName.collectAsState() else remember { mutableStateOf("User") }
    val colors = JadwalinTheme.colors

    var showNotifDialog by remember { mutableStateOf(false) }

    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title = { Text("Pusat Notifikasi", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Sinkronisasi Cloud: Aktif dan Terhubung", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Text("• Layanan Alarm: Siaga (Exact Alarm Enabled)", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Text("• Semua sistem berjalan normal.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifDialog = false }) {
                    Text("Tutup", color = colors.primary)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    Scaffold(
        topBar = { HomeHeader(userName, onNotificationClick = { showNotifDialog = true }) },
        containerColor = colors.background
    ) { padding ->
        when (val state = uiState) {
            is JadwalUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            is JadwalUiState.Success -> {
                val now = Calendar.getInstance()
                val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val currentIso = isoFormatter.format(now.time)
                val todayPrefix = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                
                val endOfTomorrow = (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val endOfTomorrowIso = isoFormatter.format(endOfTomorrow.time)

                val filteredList = state.data.filter { 
                    it.waktu_mulai >= currentIso && it.waktu_mulai <= endOfTomorrowIso
                }.sortedBy { it.waktu_mulai }

                val nextAlarm = filteredList.firstOrNull()

                HomeContent(
                    padding = padding,
                    nextAlarm = nextAlarm,
                    totalAlarm = state.data.size,
                    todayCount = state.data.count { it.waktu_mulai.startsWith(todayPrefix) },
                    upcomingCount = state.data.count { it.waktu_mulai > currentIso },
                    schedules = filteredList,
                    onNavigateToJadwal = onNavigateToJadwal,
                    onToggleAlarm = { alarm, active -> 
                        vm?.updateJadwal(alarm.copy(is_active = active))
                    }
                )
            }
            is JadwalUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Gagal memuat data: ${state.msg}", color = colors.error)
                }
            }
        }
    }
}

@Composable
fun HomeHeader(userName: String, onNotificationClick: () -> Unit) {
    val colors = JadwalinTheme.colors
    val formattedName = remember(userName) {
        userName.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }.ifEmpty { "User" }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = "Halo, $formattedName 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "BERANDA",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textHint,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onNotificationClick,
                color = colors.cardBackground,
                shape = CircleShape,
                shadowElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp,
                border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    Icon(
                        Icons.Outlined.Notifications, 
                        contentDescription = "Notifikasi", 
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    padding: PaddingValues,
    nextAlarm: JadwalEntity?,
    totalAlarm: Int,
    todayCount: Int,
    upcomingCount: Int,
    schedules: List<JadwalEntity>,
    onNavigateToJadwal: () -> Unit,
    onToggleAlarm: (JadwalEntity, Boolean) -> Unit
) {
    val colors = JadwalinTheme.colors
    val currentTime by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            value = System.currentTimeMillis()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Next Alarm Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.primary),
                shape = JadwalinShapes.Card,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(JadwalinTheme.gradient)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        Icon(Icons.Rounded.Alarm, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Jadwal Berikutnya",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = nextAlarm?.kategori ?: "Agenda",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = nextAlarm?.judul ?: "Tidak ada jadwal terdekat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val formattedTime = try {
                            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                            val formatter = SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale("id", "ID"))
                            formatter.format(parser.parse(nextAlarm?.waktu_mulai ?: "") ?: Date())
                        } catch (e: Exception) {
                            nextAlarm?.waktu_mulai ?: "Ketuk untuk tambah jadwal"
                        }

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val countdownText = if (nextAlarm != null) {
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                val target = parser.parse(nextAlarm.waktu_mulai)?.time ?: 0L
                                val diff = target - currentTime
                                if (diff > 0) {
                                    val hours = diff / (1000 * 60 * 60)
                                    val mins = (diff / (1000 * 60)) % 60
                                    val secs = (diff / 1000) % 60
                                    when {
                                        hours > 24 -> "${hours / 24} hari lagi"
                                        hours > 0 -> "$hours jam ${mins}m lagi"
                                        else -> "${mins}m ${secs}s lagi"
                                    }
                                } else "Segera dimulai"
                            } catch (e: Exception) { "Waktu dekat" }
                        } else "Sistem siap"

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = countdownText, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Statistik Cepat
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.Alarm, value = totalAlarm.toString(), label = "Total Agenda", tint = colors.primary)
                StatCard(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.EventNote, value = todayCount.toString(), label = "Hari Ini", tint = colors.catKuliah)
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.Upcoming, value = upcomingCount.toString(), label = "Mendatang", tint = colors.catKerja)
            }
        }

        // Section Header: Agenda Mendatang
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agenda Terdekat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                TextButton(onClick = onNavigateToJadwal) {
                    Text(text = "Lihat Semua", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (schedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    shape = JadwalinShapes.Card,
                    border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.EventAvailable, null, tint = colors.textHint, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tidak ada agenda dalam 24 jam ke depan", color = colors.textSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            items(schedules) { item ->
                CompactAlarmCard(item = item, onToggle = { onToggleAlarm(item, it) })
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, icon: ImageVector, value: String, label: String, tint: Color) {
    val colors = JadwalinTheme.colors
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = JadwalinShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.ExtraBold, 
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.labelMedium, 
                fontWeight = FontWeight.SemiBold, 
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CompactAlarmCard(item: JadwalEntity, onToggle: (Boolean) -> Unit) {
    val colors = JadwalinTheme.colors
    val time = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        formatter.format(parser.parse(item.waktu_mulai) ?: Date())
    } catch (e: Exception) {
        item.waktu_mulai
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = JadwalinShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when(item.kategori.lowercase()) {
                            "kuliah" -> colors.catKuliah.copy(alpha = 0.15f)
                            "kerja" -> colors.catKerja.copy(alpha = 0.15f)
                            else -> colors.primary.copy(alpha = 0.15f)
                        },
                        shape = CircleShape
                    ) {
                        Text(
                            text = item.kategori,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when(item.kategori.lowercase()) {
                                "kuliah" -> colors.catKuliah
                                "kerja" -> colors.catKerja
                                else -> colors.primary
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.judul,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.textHint
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Pengingat ${item.menitSebelumnya}m sebelumnya", style = MaterialTheme.typography.labelMedium, color = colors.textHint)
                }
            }
            
            Switch(
                checked = item.is_active,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary)
            )
        }
    }
}
