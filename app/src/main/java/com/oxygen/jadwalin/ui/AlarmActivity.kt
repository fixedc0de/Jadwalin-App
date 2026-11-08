package com.oxygen.jadwalin.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.oxygen.jadwalin.R
import com.oxygen.jadwalin.util.AlarmService

class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Paksa layar menyala dan tampilkan di atas lock screen (Versi Modern)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }

        setContentView(R.layout.activity_alarm)

        val judul = intent.getStringExtra("JUDUL") ?: "Jadwal Anda!"
        val id = intent.getIntExtra("ID", 0)
        
        findViewById<TextView>(R.id.tvAlarmJudul).text = "⏰ $judul"

        // 2. Tombol Matikan Alarm
        findViewById<Button>(R.id.btnMatikan).setOnClickListener {
            val stopIntent = Intent(this, AlarmService::class.java).apply { action = "STOP_ALARM" }
            startService(stopIntent)
            finish()
        }

        // 3. Tombol Snooze dengan Pilihan (5, 7, 10 Menit)
        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            val options = arrayOf("5 Menit", "7 Menit", "10 Menit")
            val minutesArray = intArrayOf(5, 7, 10)

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Pilih Waktu Tunda (Snooze)")
                .setItems(options) { _, which ->
                    val selectedMinutes = minutesArray[which]
                    val snoozeIntent = Intent(this, AlarmService::class.java).apply {
                        action = "SNOOZE_ALARM"
                        putExtra("ID", id)
                        putExtra("JUDUL", judul)
                        putExtra("MINUTES", selectedMinutes)
                    }
                    startService(snoozeIntent)
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}
