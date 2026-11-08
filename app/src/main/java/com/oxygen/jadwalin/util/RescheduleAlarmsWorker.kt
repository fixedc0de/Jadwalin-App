package com.oxygen.jadwalin.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oxygen.jadwalin.data.local.AppDatabase
import kotlinx.coroutines.flow.first

class RescheduleAlarmsWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ALARM_DEBUG", "RescheduleAlarmsWorker: Memulai penjadwalan ulang...")
        
        return try {
            val db = AppDatabase.getDatabase(context).jadwalDao()
            val allJadwal = db.getAllJadwal().first()

            allJadwal.forEach { jadwal ->
                if (jadwal.is_active) {
                    AlarmScheduler.scheduleAlarm(
                        ctx = context,
                        id = jadwal.id,
                        judul = jadwal.judul,
                        iso = jadwal.waktu_mulai,
                        menitSebelum = jadwal.menitSebelumnya,
                        isRecurring = jadwal.isRecurring
                    )
                }
            }
            
            Log.d("ALARM_DEBUG", "RescheduleAlarmsWorker: Berhasil menjadwalkan ${allJadwal.size} alarm.")
            Result.success()
        } catch (e: Exception) {
            Log.e("ALARM_DEBUG", "RescheduleAlarmsWorker: Gagal!", e)
            Result.retry()
        }
    }
}
