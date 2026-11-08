package com.oxygen.jadwalin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.oxygen.jadwalin.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra("ID", 0)
        val judul = intent.getStringExtra("JUDUL") ?: "Jadwal"
        val iso = intent.getStringExtra("ISO") ?: ""
        val menitSebelum = intent.getIntExtra("MENIT_SEBELUM", 0)
        val isRecurring = intent.getBooleanExtra("IS_RECURRING", false)

        Log.d("ALARM_DEBUG", "Alarm Pemicu Diterima! Menjalankan Service untuk: $judul")

        // Menjalankan AlarmService sebagai Foreground Service
        val serviceIntent = Intent(ctx, AlarmService::class.java).apply {
            putExtra("ID", id)
            putExtra("JUDUL", judul)
            putExtra("ISO", iso)
            putExtra("MENIT_SEBELUM", menitSebelum)
            putExtra("IS_RECURRING", isRecurring)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(serviceIntent)
        } else {
            ctx.startService(serviceIntent)
        }

        // Jika rutin, jadwalkan minggu depan dengan ISO yang diperbarui
        if (isRecurring && iso.isNotEmpty()) {
            try {
                // Parsing ISO untuk menambah 1 minggu secara eksplisit
                val isoFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                val date = isoFormatter.parse(iso)
                if (date != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = date }
                    cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                    val nextIso = isoFormatter.format(cal.time)
                    
                    AlarmScheduler.scheduleAlarm(ctx, id, judul, nextIso, menitSebelum, true)
                    Log.d("ALARM_DEBUG", "Alarm rutin '$judul' dijadwalkan ulang ke: $nextIso")

                    // Update database lokal agar waktu_mulai selalu tersinkron
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dao = AppDatabase.getDatabase(ctx).jadwalDao()
                            val list = dao.getAllJadwal().first()
                            val target = list.find { it.id == id }
                            if (target != null) {
                                dao.updateJadwal(target.copy(waktu_mulai = nextIso))
                            }
                        } catch (e: Exception) {
                            Log.e("ALARM_DEBUG", "Gagal memperbarui DB lokal untuk jadwal rutin: ${e.message}")
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ALARM_DEBUG", "Gagal menjadwalkan ulang alarm rutin: ${e.message}")
            }
        } else if (!isRecurring) {
            // Untuk jadwal sekali (non-recurring), saat alarm dipicu/berbunyi,
            // ubah status is_active menjadi false di database lokal
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getDatabase(ctx).jadwalDao()
                    val list = dao.getAllJadwal().first()
                    val target = list.find { it.id == id }
                    if (target != null && target.is_active) {
                        dao.updateJadwal(target.copy(is_active = false))
                        Log.d("ALARM_DEBUG", "Jadwal sekali '$judul' telah berbunyi, status otomatis diubah menjadi Nonaktif (is_active = false).")
                    }
                } catch (e: Exception) {
                    Log.e("ALARM_DEBUG", "Gagal memperbarui status nonaktif jadwal sekali: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
