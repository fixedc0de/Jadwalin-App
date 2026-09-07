package com.oxygen.jadwalin.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {
    private const val TAG = "ALARM_DEBUG"
    private val isoFormatter get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    fun scheduleAlarm(ctx: Context, id: Int, judul: String, iso: String, menitSebelum: Int, isRecurring: Boolean) {
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Hitung waktu trigger (Waktu Mulai - Menit Sebelum)
        val cal = parseIso(iso) ?: return
        val originalEventTime = cal.timeInMillis
        cal.add(Calendar.MINUTE, -menitSebelum)
        
        val now = System.currentTimeMillis()

        // Logika penanganan waktu yang sudah lewat
        if (cal.timeInMillis <= now) {
            if (isRecurring) {
                // Untuk jadwal berulang, jika sudah lewat, cari jadwal berikutnya di minggu depan
                while (cal.timeInMillis <= now) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
                Log.d(TAG, "Waktu rutin sudah lewat, dijadwalkan ulang ke minggu depan: ${cal.time}")
            } else {
                if (originalEventTime > now) {
                    // Waktu pengingat (Reminder) sudah lewat, tapi event belum mulai.
                    // Bunyikan 1 detik lagi sebagai pemberitahuan mendesak.
                    cal.timeInMillis = now + 1000
                    Log.d(TAG, "Waktu pengingat sudah lewat, disetel ulang ke: ${cal.time}")
                } else {
                    // Event aslinya sudah lewat dan tidak berulang.
                    Log.w(TAG, "Jadwal '$judul' sudah lewat sepenuhnya, tidak menjadwalkan alarm.")
                    return
                }
            }
        }
        
        Log.d(TAG, "Menjadwalkan alarm untuk: $judul pada: ${cal.time}")

        // 2. Intent ke AlarmReceiver
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra("ID", id)
            putExtra("JUDUL", judul)
            putExtra("ISO", iso)
            putExtra("MENIT_SEBELUM", menitSebelum)
            putExtra("IS_RECURRING", isRecurring)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            ctx, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Penjadwalan dengan penanganan izin yang lebih kuat
        try {
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // canScheduleExactAlarms() mengembalikan true jika memiliki SCHEDULE_EXACT_ALARM atau USE_EXACT_ALARM
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canScheduleExact) {
                // Gunakan setAlarmClock untuk presisi maksimal (terlihat di sistem sebagai alarm aktif)
                val alarmClockInfo = AlarmManager.AlarmClockInfo(cal.timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d(TAG, "Alarm dijadwalkan tepat waktu menggunakan setAlarmClock")
            } else {
                // Fallback jika tidak ada izin: Gunakan alarm tidak tepat waktu tapi tetap berjalan di Doze mode
                Log.w(TAG, "Izin EXACT_ALARM tidak ada. Menggunakan fallback setAndAllowWhileIdle.")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Gagal menjadwalkan alarm tepat waktu (SecurityException). Mencoba fallback.", e)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback juga gagal: ${ex.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menjadwalkan alarm: ${e.message}", e)
        }
    }

    fun cancelAlarm(ctx: Context, id: Int) {
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            ctx, id, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun parseIso(iso: String): Calendar? {
        return try {
            val date = isoFormatter.parse(iso) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            Log.e(TAG, "Format ISO salah: $iso", e)
            null
        }
    }
}