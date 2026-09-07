package com.oxygen.jadwalin

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.oxygen.jadwalin.data.local.AppDatabase
import com.oxygen.jadwalin.data.local.AppPreferences
import com.oxygen.jadwalin.data.local.JadwalEntity
import com.oxygen.jadwalin.data.local.ThemePreferences
import com.oxygen.jadwalin.data.remote.JadwalApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.JadwalRepository
import com.oxygen.jadwalin.databinding.ActivityMainBinding
import com.oxygen.jadwalin.databinding.DialogTambahJadwalBinding
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModelFactory
import com.oxygen.jadwalin.util.AlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var vm: JadwalViewModel
    private lateinit var tp: ThemePreferences
    private lateinit var appPrefs: AppPreferences

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // After one permission is handled, check for the next one
        checkPermissionsSequentially()
    }

    override fun onCreate(s: Bundle?) {
        // 1. Setup Preferensi & Tema (Dark/Light Mode)
        appPrefs = AppPreferences(this)
        tp = ThemePreferences(this)
        val mode = runBlocking { tp.themeModeFlow.first() }
        AppCompatDelegate.setDefaultNightMode(
            when(mode) {
                ThemePreferences.MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemePreferences.MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )

        super.onCreate(s)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Setup window insets and appearance
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Setup ViewModel
        val d = AppDatabase.getDatabase(this).jadwalDao()
        val api = NetworkClient.getRetrofit(this).create(JadwalApi::class.java)
        val r = JadwalRepository(d, api)
        vm = androidx.lifecycle.ViewModelProvider(this, JadwalViewModelFactory(r))[JadwalViewModel::class.java]

        // 3. Setup Navigation
        val nhf = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val nc = nhf.navController
        b.bottomNav.setupWithNavController(nc)

        // Sembunyikan BottomNav di Login/Register
        nc.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.registerFragment) {
                b.bottomNav.visibility = View.GONE
                b.fabTambah.visibility = View.GONE
            } else {
                b.bottomNav.visibility = View.VISIBLE
                b.fabTambah.visibility = View.VISIBLE
            }
        }

        // Cek Login secara otomatis
        lifecycleScope.launch {
            val token = appPrefs.tokenFlow.first()
            if (token == null) {
                nc.navigate(R.id.loginFragment)
            } else {
                // Sinkronisasi total saat buka aplikasi
                vm.syncData()
            }
        }

        // Reset ke menu settings utama saat tab settings diklik lagi
        b.bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.settingsFragment) {
                nc.popBackStack(R.id.settingsFragment, false)
            }
        }

        // Tangani navigasi jika user klik tab settings saat berada di sub-halaman
        b.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.settingsFragment) {
                // Jika sudah ada di flow settings, pastikan balik ke root settings
                nc.popBackStack(R.id.settingsFragment, false)
                nc.navigate(R.id.settingsFragment)
            } else {
                nc.navigate(item.itemId)
            }
            true
        }

        // 4. Setup FAB
        b.fabTambah.setOnClickListener { showAddDialog() }

        // 5. Cek & Request Permission secara berurutan
        checkPermissionsSequentially()
    }

    private fun checkPermissionsSequentially() {
        when {
            // 1. Notifikasi (Android 13+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            // 2. Audio (Android 13+) - Diperlukan untuk akses file suara kustom
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED -> {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }

            // 3. Optimasi Baterai
            else -> {
                if (!checkBatteryOptimization()) {
                    // Jika dialog baterai tidak muncul/sudah beres, lanjut ke exact alarm
                    checkExactAlarmPermission()
                }
            }
        }
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Optimasi Baterai Aktif")
                    .setMessage("Agar alarm selalu berbunyi tepat waktu, mohon nonaktifkan optimasi baterai untuk aplikasi ini.")
                    .setCancelable(false)
                    .setPositiveButton("Buka Pengaturan") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                    .setNegativeButton("Nanti") { _, _ ->
                        checkExactAlarmPermission()
                    }
                    .show()
                return true
            }
        }
        return false
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Izin Alarm Tepat Waktu")
                    .setMessage("Aplikasi ini membutuhkan izin untuk menjadwalkan alarm tepat waktu agar pengingat berfungsi dengan baik.\n\nTanpa izin ini, alarm mungkin akan tertunda beberapa menit.")
                    .setCancelable(false)
                    .setPositiveButton("Buka Pengaturan") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("Nanti", null)
                    .show()
            }
        }
    }

    private fun showAddDialog() {
        val dlg = BottomSheetDialog(this, R.style.Theme_Jadwalin_BottomSheetDialog)
        val db = DialogTambahJadwalBinding.inflate(layoutInflater)
        dlg.setContentView(db.root)

        // Gaya iOS biasanya full screen atau sangat tinggi
        dlg.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED

        var selectedKategori = "Kuliah"
        var selectedHari = -1 // -1 = Tidak berulang
        val selectedDate = Calendar.getInstance()

        fun updateTanggalVisibility() {
            val isLainnya = selectedKategori == "Lainnya"
            db.rowTanggal.visibility = if (isLainnya) View.VISIBLE else View.GONE
            db.dividerTanggal.visibility = if (isLainnya) View.VISIBLE else View.GONE
            
            // Jika bukan 'Lainnya', reset ke hari ini agar logika jam (besok) tetap jalan
            if (!isLainnya) {
                selectedDate.timeInMillis = System.currentTimeMillis()
                db.tvTanggalLabel.text = "Hari Ini"
            }
        }

        db.rowKategori.setOnClickListener {
            val items = arrayOf("Kuliah", "Sekolah", "Kerja", "Lainnya")
            AlertDialog.Builder(this)
                .setTitle("Pilih Kategori")
                .setItems(items) { _, which ->
                    selectedKategori = items[which]
                    db.tvKategoriLabel.text = selectedKategori
                    updateTanggalVisibility()
                }.show()
        }

        db.rowTanggal.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d)
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                db.tvTanggalLabel.text = sdf.format(selectedDate.time)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        db.rowUlangi.setOnClickListener {
            val items = arrayOf("Jangan Pernah", "Setiap Minggu", "Setiap Senin", "Setiap Selasa", "Setiap Rabu", "Setiap Kamis", "Setiap Jumat", "Setiap Sabtu")
            AlertDialog.Builder(this)
                .setTitle("Ulangi")
                .setItems(items) { _, which ->
                    selectedHari = which - 1 // Mapping: 0="Jangan Pernah" (-1), 1="Minggu" (0), dst.
                    db.tvUlangiLabel.text = items[which]
                }.show()
        }

        db.btnSimpan.setOnClickListener {
            val judul = db.etJudul.text.toString().trim().ifEmpty { "Jadwal" }
            
            val cal = Calendar.getInstance()
            // Gunakan tanggal yang dipilih (jika kategori 'Lainnya')
            if (selectedKategori == "Lainnya") {
                cal.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
                cal.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
                cal.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
            }

            cal.set(Calendar.HOUR_OF_DAY, db.tpWaktu.hour)
            cal.set(Calendar.MINUTE, db.tpWaktu.minute)
            cal.set(Calendar.SECOND, 0)

            if (selectedHari != -1) {
                // Mapping: 0=Minggu (Calendar.SUNDAY=1), 1=Senin (Calendar.MONDAY=2)
                val targetDay = selectedHari + 1
                while (cal.get(Calendar.DAY_OF_WEEK) != targetDay) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                // Jika sudah lewat jamnya hari ini (tapi harinya sama), lompat ke minggu depan
                if (cal.timeInMillis < System.currentTimeMillis()) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            } else {
                // Jika tidak berulang dan waktu sudah lewat hari ini, set ke besok
                if (cal.timeInMillis < System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val isoString = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.time)
            // Gunakan hash atau modulo agar ID unik dan masuk range Int
            val id = (System.currentTimeMillis() % 1000000000).toInt()
            
            val j = JadwalEntity(
                id = id,
                judul = judul,
                deskripsi = null,
                waktu_mulai = isoString,
                kategori = selectedKategori,
                lokasi = null,
                isRecurring = selectedHari != -1,
                hariDalamMinggu = selectedHari,
                menitSebelumnya = if (db.swTunda.isChecked) 10 else 0 // Contoh nilai default
            )

            vm.simpanJadwal(j)

            lifecycleScope.launch {
                val menitDefault = appPrefs.reminderMinutesFlow.first()
                AlarmScheduler.scheduleAlarm(
                    ctx = this@MainActivity,
                    id = id,
                    judul = judul,
                    iso = isoString,
                    menitSebelum = if (db.swTunda.isChecked) menitDefault else 0,
                    isRecurring = selectedHari != -1
                )
            }

            Toast.makeText(this, "Jadwal disimpan!", Toast.LENGTH_SHORT).show()
            dlg.dismiss()
        }

        db.btnBatal.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }
}