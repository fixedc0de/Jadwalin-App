package com.oxygen.jadwalin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.oxygen.jadwalin.R
import com.oxygen.jadwalin.data.local.AppDatabase
import com.oxygen.jadwalin.data.local.AppPreferences
import com.oxygen.jadwalin.data.local.ThemePreferences
import com.oxygen.jadwalin.data.remote.AuthApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.AuthRepository
import com.oxygen.jadwalin.ui.compose.JadwalinTheme
import com.oxygen.jadwalin.ui.compose.SettingsScreen
import com.oxygen.jadwalin.ui.viewmodel.*

class SettingsFragment : Fragment() {

    private val themeVm: ThemeViewModel by activityViewModels {
        ThemeViewModelFactory(ThemePreferences(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val authApi = NetworkClient.getRetrofit(requireContext()).create(AuthApi::class.java)
        val prefs = AppPreferences(requireContext())
        val db = AppDatabase.getDatabase(requireContext()).jadwalDao()
        val authRepo = AuthRepository(authApi, prefs, db, requireContext())
        val authVm = ViewModelProvider(this, AuthViewModelFactory(authRepo))[AuthViewModel::class.java]
        val settingsVm = ViewModelProvider(this, SettingsViewModelFactory(prefs))[SettingsViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                JadwalinTheme {
                    SettingsScreen(
                        themeVm = themeVm,
                        authVm = authVm,
                        settingsVm = settingsVm,
                        onLogout = { 
                            authVm.logout {
                                try {
                                    findNavController().navigate(R.id.action_global_to_login)
                                } catch (_: Exception) {
                                    findNavController().navigate(R.id.loginFragment)
                                }
                            }
                        },
                        onNavigateToDevInfo = { findNavController().navigate(R.id.devInfoFragment) },
                        onNavigateToAppInfo = { findNavController().navigate(R.id.appInfoFragment) }
                    )
                }
            }
        }
    }
}
