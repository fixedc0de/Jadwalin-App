package com.oxygen.jadwalin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

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

        // Jika rutin, jadwalkan minggu depan
        if (isRecurring && iso.isNotEmpty()) {
            try {
                AlarmScheduler.scheduleAlarm(ctx, id, judul, iso, menitSebelum, true)
            } catch (e: Exception) {
                Log.e("ALARM_DEBUG", "Gagal menjadwalkan ulang alarm rutin: ${e.message}")
            }
        }
    }
}
