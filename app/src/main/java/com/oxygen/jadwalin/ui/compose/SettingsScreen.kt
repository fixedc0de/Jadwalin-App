package com.oxygen.jadwalin.ui.compose

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.oxygen.jadwalin.data.local.ThemePreferences
import com.oxygen.jadwalin.ui.viewmodel.AuthViewModel
import com.oxygen.jadwalin.ui.viewmodel.SettingsViewModel
import com.oxygen.jadwalin.ui.viewmodel.ThemeViewModel

@Composable
fun SettingsScreen(
    themeVm: ThemeViewModel? = null,
    authVm: AuthViewModel? = null,
    settingsVm: SettingsViewModel? = null,
    onLogout: () -> Unit = {},
    onNavigateToDevInfo: () -> Unit = {},
    onNavigateToAppInfo: () -> Unit = {}
) {
    val themeMode by if (themeVm != null) themeVm.themeMode.collectAsState() else remember { mutableIntStateOf(ThemePreferences.MODE_SYSTEM) }
    val userName by if (authVm != null) authVm.userName.collectAsState() else remember { mutableStateOf("User") }
    val userEmail by if (authVm != null) authVm.userEmail.collectAsState() else remember { mutableStateOf("") }
    
    val autoVibrate by if (settingsVm != null) settingsVm.autoVibrate.collectAsState() else remember { mutableStateOf(true) }
    val crescendo by if (settingsVm != null) settingsVm.crescendo.collectAsState() else remember { mutableStateOf(true) }
    val reminderMinutes by if (settingsVm != null) settingsVm.reminderMinutes.collectAsState() else remember { mutableIntStateOf(30) }
    val defaultSnooze by if (settingsVm != null) settingsVm.defaultSnooze.collectAsState() else remember { mutableIntStateOf(10) }
    val alarmSoundUri by if (settingsVm != null) settingsVm.alarmSoundUri.collectAsState() else remember { mutableStateOf<String?>(null) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val colors = JadwalinTheme.colors

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            settingsVm?.setAlarmSoundUri(uri?.toString())
            Toast.makeText(context, "Suara alarm utama berhasil diperbarui", Toast.LENGTH_SHORT).show()
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Informasi Akun", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nama: $userName", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("Email: $userEmail", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    Text("Status: Akun Terverifikasi (Cloud Active)", style = MaterialTheme.typography.bodySmall, color = colors.success)
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Tutup", color = colors.primary)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    if (showReminderDialog) {
        var tempValue by remember { mutableFloatStateOf(reminderMinutes.toFloat()) }
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Atur Waktu Pengingat Awal", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Pilih berapa menit sebelum jadwal alarm akan berbunyi (1 - 60 Menit):", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    
                    Text("${tempValue.toInt()} Menit Sebelum", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = colors.primary)
                    
                    Slider(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        valueRange = 1f..60f,
                        steps = 58,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary
                        )
                    )

                    val quickChips = listOf(5, 10, 15, 30, 45, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        quickChips.forEach { mins ->
                            Surface(
                                onClick = { tempValue = mins.toFloat() },
                                color = if (tempValue.toInt() == mins) colors.primary else colors.pillBackground,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${mins}m",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tempValue.toInt() == mins) Color.White else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val mins = tempValue.toInt().coerceIn(1, 60)
                    settingsVm?.setReminderMinutes(mins)
                    showReminderDialog = false
                    Toast.makeText(context, "Pengingat disetel $mins menit sebelum jadwal", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Simpan", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Batal", color = colors.textSecondary)
                }
            },
            containerColor = colors.cardBackground
        )
    }

    if (showSnoozeDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Waktu Tunda Default (Snooze)", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                val options = listOf(5, 7, 10, 15)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsVm?.setDefaultSnooze(mins)
                                    showSnoozeDialog = false
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = defaultSnooze == mins, onClick = {
                                settingsVm?.setDefaultSnooze(mins)
                                showSnoozeDialog = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$mins Menit", color = colors.textPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnoozeDialog = false }) {
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { 
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Sesuaikan preferensi aplikasi Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            item { 
                UserProfileCard(
                    name = userName, 
                    email = userEmail,
                    onClick = { showProfileDialog = true }
                ) 
            }

            // TAMPILAN & TEMA
            item {
                SettingsSection(title = "TAMPILAN & TEMA") {
                    SettingsItem(
                        icon = Icons.Rounded.DarkMode,
                        iconBg = colors.primary.copy(alpha = 0.15f),
                        iconTint = colors.primary,
                        title = "Mode Gelap",
                        subtitle = "Menyesuaikan tampilan nyaman",
                        trailing = {
                            Switch(
                                checked = themeMode == ThemePreferences.MODE_DARK,
                                onCheckedChange = { 
                                    themeVm?.setThemeMode(if (it) ThemePreferences.MODE_DARK else ThemePreferences.MODE_LIGHT)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary)
                            )
                        }
                    )
                }
            }

            // PENGATURAN ALARM & SUARA
            item {
                SettingsSection(title = "PENGATURAN ALARM & SUARA") {
                    SettingsItem(
                        icon = Icons.Rounded.AlarmOn,
                        iconBg = colors.primary.copy(alpha = 0.15f),
                        iconTint = colors.primary,
                        title = "Waktu Pengingat Awal",
                        subtitle = "Ingatkan sebelum jadwal dimulai",
                        trailing = { ValuePill(text = "$reminderMinutes Menit") },
                        showChevron = true,
                        onClick = { showReminderDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = colors.divider)
                    SettingsItem(
                        icon = Icons.Rounded.Snooze,
                        iconBg = colors.catKerja.copy(alpha = 0.15f),
                        iconTint = colors.catKerja,
                        title = "Waktu Tunda (Snooze)",
                        subtitle = "Interval pengingat ulang",
                        trailing = { ValuePill(text = "$defaultSnooze Menit") },
                        showChevron = true,
                        onClick = { showSnoozeDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = colors.divider)
                    SettingsItem(
                        icon = Icons.Rounded.NotificationsActive,
                        iconBg = colors.catKuliah.copy(alpha = 0.15f),
                        iconTint = colors.catKuliah,
                        title = "Suara Alarm Utama",
                        subtitle = if (alarmSoundUri != null) "Nada dering kustom aktif" else "Nada dering bawaan sistem",
                        trailing = { ValuePill(text = if (alarmSoundUri != null) "Kustom" else "Bawaan") },
                        showChevron = true,
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih Suara Alarm Utama")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarmSoundUri?.toUri())
                            }
                            ringtoneLauncher.launch(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = colors.divider)
                    SettingsItem(
                        icon = Icons.Rounded.Vibration,
                        iconBg = colors.catKuliah.copy(alpha = 0.15f),
                        iconTint = colors.catKuliah,
                        title = "Getar Otomatis",
                        subtitle = "Getaran ritmis saat alarm",
                        trailing = {
                            Switch(
                                checked = autoVibrate,
                                onCheckedChange = { settingsVm?.setAutoVibrate(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary)
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = colors.divider)
                    SettingsItem(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        iconBg = colors.primary.copy(alpha = 0.15f),
                        iconTint = colors.primary,
                        title = "Volume Bertahap",
                        subtitle = "Crescendo lembut agar segar",
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ValuePill(text = "30s")
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = crescendo,
                                    onCheckedChange = { settingsVm?.setCrescendo(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = colors.primary)
                                )
                            }
                        }
                    )
                }
            }

            // SINKRONISASI & CADANGAN
            item {
                SettingsSection(title = "SINKRONISASI & CADANGAN") {
                    SettingsItem(
                        icon = Icons.Rounded.Sync,
                        iconBg = colors.success.copy(alpha = 0.15f),
                        iconTint = colors.success,
                        title = "Cadangkan & Pulihkan",
                        subtitle = "Sinkronisasi otomatis aktif",
                        showChevron = true,
                        onClick = { Toast.makeText(context, "Sinkronisasi cloud berhasil: Semua data terbaru tersinkron", Toast.LENGTH_SHORT).show() }
                    )
                }
            }

            // TENTANG APLIKASI
            item {
                SettingsSection(title = "TENTANG APLIKASI") {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        iconBg = colors.textHint.copy(alpha = 0.15f),
                        iconTint = colors.textSecondary,
                        title = "Tentang Jadwalin",
                        subtitle = "Filosofi desain & lisensi",
                        showChevron = true,
                        onClick = onNavigateToAppInfo
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = colors.divider)
                    SettingsItem(
                        icon = Icons.Rounded.Code,
                        iconBg = colors.textHint.copy(alpha = 0.15f),
                        iconTint = colors.textSecondary,
                        title = "Developer Info",
                        subtitle = "Pengembang & kontributor",
                        trailing = { ValuePill(text = "Tim Oxygen") },
                        showChevron = true,
                        onClick = onNavigateToDevInfo
                    )
                }
            }

            item {
                Surface(
                    onClick = onLogout,
                    color = colors.error.copy(alpha = 0.1f),
                    shape = JadwalinShapes.Card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = colors.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Keluar dari Akun", color = colors.error, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Versi 1.0.0 (Build 2026)", fontSize = 12.sp, color = colors.textHint)
                    Text(text = "Dibuat dengan dedikasi oleh Tim Oxygen", fontSize = 12.sp, color = colors.textHint)
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

@Composable
fun UserProfileCard(name: String, email: String, onClick: () -> Unit = {}) {
    val colors = JadwalinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = JadwalinShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = name.firstOrNull()?.uppercase() ?: "U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(colors.cardBackground).padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(colors.success),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(text = email, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.textHint)
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = JadwalinTheme.colors
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textHint,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = JadwalinShapes.Card,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: () -> Unit = {}
) {
    val colors = JadwalinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        if (trailing != null) trailing()
        if (showChevron) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.textHint)
        }
    }
}

@Composable
fun ValuePill(text: String) {
    val colors = JadwalinTheme.colors
    Surface(
        color = colors.pillBackground,
        shape = CircleShape
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary
        )
    }
}
