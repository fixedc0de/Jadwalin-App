package com.oxygen.jadwalin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.oxygen.jadwalin.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("ALARM_DEBUG", "HP Berhasil Restart. Menjadwalkan ulang semua alarm...")
            
            val db = AppDatabase.getDatabase(ctx).jadwalDao()

            CoroutineScope(Dispatchers.IO).launch {
                val allJadwal = db.getAllJadwal().first()

                allJadwal.forEach { jadwal ->
                    AlarmScheduler.scheduleAlarm(
                        ctx = ctx,
                        id = jadwal.id,
                        judul = jadwal.judul,
                        iso = jadwal.waktu_mulai,
                        menitSebelum = jadwal.menitSebelumnya,
                        isRecurring = jadwal.isRecurring
                    )
                }
            }
        }
    }
}
