package com.oxygen.jadwalin.ui.fragments

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oxygen.jadwalin.R
import com.oxygen.jadwalin.data.local.AppPreferences
import com.oxygen.jadwalin.data.local.ThemePreferences
import com.oxygen.jadwalin.data.remote.AuthApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.AuthRepository
import com.oxygen.jadwalin.databinding.FragmentSettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsFragment : Fragment() {
    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    private lateinit var themePrefs: ThemePreferences
    private lateinit var appPrefs: AppPreferences
    private lateinit var authRepo: AuthRepository

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }

            // Persist permission for the picked URI
            uri?.let {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(it, flags)
                } catch (e: Exception) {
                    // Fail silently for system ringtones that don't support persistable permissions
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                appPrefs.saveAlarmSoundUri(uri?.toString())
                updateRingtoneName(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentSettingsBinding.inflate(inflater, container, false)
        themePrefs = ThemePreferences(requireContext())
        appPrefs = AppPreferences(requireContext())
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Logika Mode Gelap
        viewLifecycleOwner.lifecycleScope.launch {
            themePrefs.themeModeFlow.collect { mode ->
                b.switchDarkMode.isChecked = (mode == ThemePreferences.MODE_DARK)
            }
        }
        b.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                themePrefs.saveThemeMode(if (isChecked) ThemePreferences.MODE_DARK else ThemePreferences.MODE_LIGHT)
            }
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }

        // 2. Logika Waktu Pengingat Default (Grouped iOS Style)
        val reminderOptions = arrayOf("15 menit", "30 menit", "60 menit")
        
        viewLifecycleOwner.lifecycleScope.launch {
            val currentMinutes = appPrefs.reminderMinutesFlow.first()
            b.tvReminderValue.text = when (currentMinutes) {
                15 -> "15 menit"
                60 -> "60 menit"
                else -> "30 menit"
            }
        }

        b.rowReminder.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Waktu Pengingat")
                .setItems(reminderOptions) { _, which ->
                    val selectedMinutes = when (which) {
                        0 -> 15
                        2 -> 60
                        else -> 30
                    }
                    b.tvReminderValue.text = reminderOptions[which]
                    viewLifecycleOwner.lifecycleScope.launch {
                        appPrefs.saveReminderMinutes(selectedMinutes)
                    }
                }.show()
        }

        // 3. Logika Suara Alarm
        viewLifecycleOwner.lifecycleScope.launch {
            val soundUriStr = appPrefs.alarmSoundUriFlow.first()
            val soundUri = soundUriStr?.toUri()
            updateRingtoneName(soundUri)
        }

        b.rowAlarmSound.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih Suara Alarm")
                val currentUriStr = runBlocking { appPrefs.alarmSoundUriFlow.first() }
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUriStr?.toUri())
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            }
            ringtonePickerLauncher.launch(intent)
        }

        b.rowAppInfo.setOnClickListener {
            findNavController().navigate(R.id.appInfoFragment)
        }

        b.rowDevInfo.setOnClickListener {
            findNavController().navigate(R.id.devInfoFragment)
        }

        // 4. Logout
        val api = NetworkClient.getRetrofit(requireContext()).create(AuthApi::class.java)
        authRepo = AuthRepository(api, appPrefs)
        
        b.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Keluar Akun")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Keluar") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        authRepo.logout()
                        // Restart Activity atau navigasi balik ke Login
                        val intent = requireActivity().intent
                        requireActivity().finish()
                        startActivity(intent)
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun updateRingtoneName(uri: Uri?) {
        val name = if (uri == null) {
            "Default"
        } else {
            val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
            ringtone?.getTitle(requireContext()) ?: "Unknown"
        }
        b.tvAlarmSoundValue.text = name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
