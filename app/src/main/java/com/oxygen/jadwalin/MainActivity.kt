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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.oxygen.jadwalin.databinding.DialogEditJadwalBinding
import com.oxygen.jadwalin.databinding.DialogTambahJadwalBinding
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModelFactory
import com.oxygen.jadwalin.ui.viewmodel.ThemeViewModel
import com.oxygen.jadwalin.ui.viewmodel.ThemeViewModelFactory
import com.oxygen.jadwalin.util.AlarmScheduler
import com.oxygen.jadwalin.util.RescheduleAlarmsWorker
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
    private var activePermissionDialog: AlertDialog? = null

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
        val initialNightMode = when(mode) {
            ThemePreferences.MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemePreferences.MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(initialNightMode)

        var lastAppliedNightMode = initialNightMode
        val themeVm = ViewModelProvider(this, ThemeViewModelFactory(tp))[ThemeViewModel::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                themeVm.themeMode.collect { currentMode ->
                    val nightMode = when(currentMode) {
                        ThemePreferences.MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        ThemePreferences.MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    if (nightMode != lastAppliedNightMode) {
                        lastAppliedNightMode = nightMode
                        AppCompatDelegate.setDefaultNightMode(nightMode)
                    }
                }
            }
        }

        super.onCreate(s)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Setup window insets and appearance
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Tangani insets untuk Floating Dock agar tidak terpotong Navbar sistem
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(b.dockContainer) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.bottomMargin = systemBars.bottom + (24 * resources.displayMetrics.density).toInt()
            view.layoutParams = params
            insets
        }

        // 2. Setup ViewModel
        val d = AppDatabase.getDatabase(this).jadwalDao()
        val api = NetworkClient.getRetrofit(this).create(JadwalApi::class.java)
        val r = JadwalRepository(d, api)
        vm = androidx.lifecycle.ViewModelProvider(this, JadwalViewModelFactory(r))[JadwalViewModel::class.java]

        // Sinkronisasi dan penjadwalan ulang alarm aktif saat startup aplikasi
        androidx.work.WorkManager.getInstance(this).enqueue(
            androidx.work.OneTimeWorkRequestBuilder<RescheduleAlarmsWorker>().build()
        )

        // 3. Setup Navigation
        val nhf = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val nc = nhf.navController
        b.bottomNav.setupWithNavController(nc)

        // Sembunyikan BottomNav di Login/Register
        nc.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.registerFragment) {
                b.dockContainer.visibility = View.GONE
                b.fabTambah.visibility = View.GONE
            } else {
                b.dockContainer.visibility = View.VISIBLE
                b.fabTambah.visibility = View.VISIBLE
            }
        }

        // Cek Login secara otomatis
        lifecycleScope.launch {
            val token = appPrefs.tokenFlow.first()
            if (token == null) {
                nc.navigate(R.id.loginFragment)
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

        // 5. Cek & Request Permission secara berurutan hanya pada launch pertama
        if (s == null) {
            checkPermissionsSequentially()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activePermissionDialog?.dismiss()
        activePermissionDialog = null
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
                activePermissionDialog?.dismiss()
                activePermissionDialog = AlertDialog.Builder(this)
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
            try {
                if (!alarmManager.canScheduleExactAlarms()) {
                    activePermissionDialog?.dismiss()
                    activePermissionDialog = AlertDialog.Builder(this)
                        .setTitle("Izin Alarm Presisi")
                        .setMessage("Jadwalin membutuhkan izin 'Alarm Tepat Waktu' agar pengingat jadwal Anda berbunyi tepat pada waktunya.\n\nTanpa izin ini, sistem mungkin menunda alarm untuk menghemat baterai.")
                        .setCancelable(false)
                        .setPositiveButton("Buka Pengaturan") { _, _ ->
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback jika intent spesifik gagal
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            }
                        }
                        .setNegativeButton("Nanti") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ALARM_DEBUG", "Gagal mengecek izin alarm: ${e.message}")
            }
        }
    }

    fun showAddDialog() {
        val dlg = BottomSheetDialog(this, R.style.Theme_Jadwalin_BottomSheetDialog)
        val db = DialogTambahJadwalBinding.inflate(layoutInflater)
        dlg.setContentView(db.root)

        // Format Waktu 24 Jam (00.00 - 23.59) tanpa AM/PM
        db.tpWaktu.setIs24HourView(true)

        // Gaya iOS biasanya full screen atau sangat tinggi
        dlg.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED

        var selectedKategori = "Kuliah"
        var selectedHari = -1 // -1 = Tidak berulang
        val selectedDate = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        db.tvTanggalLabel.text = sdfDate.format(selectedDate.time)

        db.rowKategori.setOnClickListener {
            val items = arrayOf("Kuliah", "Sekolah", "Kerja", "Lainnya")
            AlertDialog.Builder(this)
                .setTitle("Pilih Kategori")
                .setItems(items) { _, which ->
                    selectedKategori = items[which]
                    db.tvKategoriLabel.text = selectedKategori
                }.show()
        }

        db.rowTanggal.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d)
                db.tvTanggalLabel.text = sdfDate.format(selectedDate.time)
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
            // Gunakan tanggal yang dipilih pengguna (tahun, bulan, tanggal) untuk semua kategori
            cal.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
            cal.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))

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
                // Jika tidak berulang dan jam yang dipilih pada hari ini sudah lewat, lompat ke besok
                val now = Calendar.getInstance()
                val isToday = (selectedDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        selectedDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
                if (isToday && cal.timeInMillis < System.currentTimeMillis()) {
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

    fun showEditDialog(jadwal: JadwalEntity) {
        val dlg = BottomSheetDialog(this, R.style.Theme_Jadwalin_BottomSheetDialog)
        val db = DialogEditJadwalBinding.inflate(layoutInflater)
        dlg.setContentView(db.root)

        dlg.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED

        // Format Waktu 24 Jam (00.00 - 23.59) tanpa AM/PM
        db.tpWaktu.setIs24HourView(true)

        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val cal = Calendar.getInstance()
        try {
            val date = parser.parse(jadwal.waktu_mulai)
            if (date != null) cal.time = date
        } catch (_: Exception) {}

        db.tpWaktu.hour = cal.get(Calendar.HOUR_OF_DAY)
        db.tpWaktu.minute = cal.get(Calendar.MINUTE)
        db.etJudul.setText(jadwal.judul)

        var selectedKategori = jadwal.kategori
        var selectedHari = jadwal.hariDalamMinggu

        val selectedDate = cal.clone() as Calendar
        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        db.tvTanggalLabel.text = sdfDate.format(selectedDate.time)

        db.rowTanggal.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d)
                db.tvTanggalLabel.text = sdfDate.format(selectedDate.time)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        db.tvKategoriLabel.text = selectedKategori
        db.swTunda.isChecked = jadwal.menitSebelumnya > 0

        val itemsUlangi = arrayOf("Jangan Pernah", "Setiap Minggu", "Setiap Senin", "Setiap Selasa", "Setiap Rabu", "Setiap Kamis", "Setiap Jumat", "Setiap Sabtu")
        db.tvUlangiLabel.text = itemsUlangi.getOrElse(selectedHari + 1) { "Jangan Pernah" }

        db.rowKategori.setOnClickListener {
            val items = arrayOf("Kuliah", "Sekolah", "Kerja", "Lainnya")
            AlertDialog.Builder(this)
                .setTitle("Pilih Kategori")
                .setItems(items) { _, which ->
                    selectedKategori = items[which]
                    db.tvKategoriLabel.text = selectedKategori
                }.show()
        }

        db.rowUlangi.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Ulangi")
                .setItems(itemsUlangi) { _, which ->
                    selectedHari = which - 1
                    db.tvUlangiLabel.text = itemsUlangi[which]
                }.show()
        }

        db.btnSimpan.setOnClickListener {
            val judul = db.etJudul.text.toString().trim().ifEmpty { "Jadwal" }
            
            val editCal = Calendar.getInstance()
            editCal.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
            editCal.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
            editCal.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))

            editCal.set(Calendar.HOUR_OF_DAY, db.tpWaktu.hour)
            editCal.set(Calendar.MINUTE, db.tpWaktu.minute)
            editCal.set(Calendar.SECOND, 0)

            if (selectedHari != -1) {
                val targetDay = selectedHari + 1
                while (editCal.get(Calendar.DAY_OF_WEEK) != targetDay) {
                    editCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (editCal.timeInMillis < System.currentTimeMillis()) {
                    editCal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            } else {
                val now = Calendar.getInstance()
                val isToday = (selectedDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        selectedDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
                if (isToday && editCal.timeInMillis < System.currentTimeMillis()) {
                    editCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val isoString = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(editCal.time)
            
            val updatedJadwal = jadwal.copy(
                judul = judul,
                waktu_mulai = isoString,
                kategori = selectedKategori,
                isRecurring = selectedHari != -1,
                hariDalamMinggu = selectedHari,
                menitSebelumnya = if (db.swTunda.isChecked) 10 else 0
            )

            vm.updateJadwal(updatedJadwal)

            lifecycleScope.launch {
                val menitDefault = appPrefs.reminderMinutesFlow.first()
                AlarmScheduler.scheduleAlarm(
                    ctx = this@MainActivity,
                    id = updatedJadwal.id,
                    judul = judul,
                    iso = isoString,
                    menitSebelum = if (db.swTunda.isChecked) menitDefault else 0,
                    isRecurring = selectedHari != -1
                )
            }

            Toast.makeText(this, "Jadwal diperbarui!", Toast.LENGTH_SHORT).show()
            dlg.dismiss()
        }

        db.btnBatal.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }
}