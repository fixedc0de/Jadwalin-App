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
import com.oxygen.jadwalin.data.remote.AuthApi
import com.oxygen.jadwalin.data.remote.JadwalApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.AuthRepository
import com.oxygen.jadwalin.data.repository.JadwalRepository
import com.oxygen.jadwalin.ui.compose.HomeScreen
import com.oxygen.jadwalin.ui.compose.JadwalinTheme
import com.oxygen.jadwalin.ui.viewmodel.AuthViewModel
import com.oxygen.jadwalin.ui.viewmodel.AuthViewModelFactory
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModelFactory

class BerandaFragment : Fragment() {

    private val vm: JadwalViewModel by activityViewModels {
        val d = AppDatabase.getDatabase(requireContext()).jadwalDao()
        val api = NetworkClient.getRetrofit(requireContext()).create(JadwalApi::class.java)
        JadwalViewModelFactory(JadwalRepository(d, api))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val authApi = NetworkClient.getRetrofit(requireContext()).create(AuthApi::class.java)
        val prefs = AppPreferences(requireContext())
        val authRepo = AuthRepository(authApi, prefs)
        val authVm = ViewModelProvider(this, AuthViewModelFactory(authRepo))[AuthViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                JadwalinTheme {
                    HomeScreen(
                        vm = vm,
                        authVm = authVm,
                        onNavigateToJadwal = { findNavController().navigate(R.id.jadwalFragment) }
                    )
                }
            }
        }
    }
}
