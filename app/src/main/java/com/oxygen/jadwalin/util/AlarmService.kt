package com.oxygen.jadwalin.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.oxygen.jadwalin.R
import com.oxygen.jadwalin.data.local.AppPreferences
import com.oxygen.jadwalin.ui.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var appPrefs: AppPreferences

    override fun onCreate() {
        super.onCreate()
        appPrefs = AppPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_ALARM") {
            stopSelf()
            return START_NOT_STICKY
        } else if (action == "SNOOZE_ALARM") {
            val id = intent.getIntExtra("ID", 0)
            val judul = intent.getStringExtra("JUDUL") ?: "Jadwal"
            snoozeAlarm(id, judul)
            stopSelf()
            return START_NOT_STICKY
        }

        val id = intent?.getIntExtra("ID", 0) ?: 0
        val judul = intent?.getStringExtra("JUDUL") ?: "Jadwal"
        val iso = intent?.getStringExtra("ISO") ?: ""
        val menitSebelum = intent?.getIntExtra("MENIT_SEBELUM", 0) ?: 0
        val isRecurring = intent?.getBooleanExtra("IS_RECURRING", false) ?: false

        showNotification(id, judul, iso, menitSebelum, isRecurring)
        startAlarm()

        return START_STICKY
    }

    private fun snoozeAlarm(id: Int, judul: String) {
        val now = java.util.Calendar.getInstance()
        // Format ISO for scheduleAlarm (add 10 minutes from now)
        now.add(java.util.Calendar.MINUTE, 10)
        val snoozeIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(now.time)

        AlarmScheduler.scheduleAlarm(
            ctx = this,
            id = id,
            judul = "$judul (Snooze)",
            iso = snoozeIso,
            menitSebelum = 0,
            isRecurring = false
        )
    }

    private fun showNotification(id: Int, judul: String, iso: String, menitSebelum: Int, isRecurring: Boolean) {
        val channelId = "jadwalin_alarm_service_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Aktif",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }

        // Action Turn Off
        val stopIntent = Intent(this, AlarmService::class.java).apply { action = "STOP_ALARM" }
        val stopPendingIntent = PendingIntent.getService(this, id, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Action Snooze
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = "SNOOZE_ALARM"
            putExtra("ID", id)
            putExtra("JUDUL", judul)
        }
        val snoozePendingIntent = PendingIntent.getService(this, id + 500, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("JUDUL", judul)
            putExtra("NOTIFICATION_ID", id)
            putExtra("ID", id)
            putExtra("ISO", iso)
            putExtra("MENIT_SEBELUM", menitSebelum)
            putExtra("IS_RECURRING", isRecurring)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(this, id, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Jadwalin: $judul")
            .setContentText("Sedang berbunyi... Ketuk untuk melihat.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .addAction(R.drawable.ic_notification, "Matikan", stopPendingIntent)
            .addAction(R.drawable.ic_notification, "Snooze (10m)", snoozePendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id + 100, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id + 100, notification)
        }
    }

    private fun startAlarm() {
        CoroutineScope(Dispatchers.Main).launch {
            val soundUriStr = appPrefs.alarmSoundUriFlow.first()
            val customUri = soundUriStr?.toUri()
            
            val success = try {
                if (customUri != null) {
                    playUri(customUri)
                    true
                } else false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

            // Fallback ke default jika gagal atau tidak ada custom URI
            if (!success) {
                try {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    playUri(defaultUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun playUri(uri: Uri) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Rethrow to trigger fallback in startAlarm
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
