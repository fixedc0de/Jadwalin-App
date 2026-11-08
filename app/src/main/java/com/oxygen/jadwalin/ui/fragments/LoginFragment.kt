package com.oxygen.jadwalin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.oxygen.jadwalin.R
import com.oxygen.jadwalin.data.local.AppDatabase
import com.oxygen.jadwalin.data.local.AppPreferences
import com.oxygen.jadwalin.data.remote.AuthApi
import com.oxygen.jadwalin.data.remote.JadwalApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.AuthRepository
import com.oxygen.jadwalin.data.repository.JadwalRepository
import com.oxygen.jadwalin.ui.compose.JadwalinTheme
import com.oxygen.jadwalin.ui.compose.LoginScreen
import com.oxygen.jadwalin.ui.viewmodel.AuthState
import com.oxygen.jadwalin.ui.viewmodel.AuthViewModel
import com.oxygen.jadwalin.ui.viewmodel.AuthViewModelFactory
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel
    private val jadwalVm: JadwalViewModel by activityViewModels {
        val d = AppDatabase.getDatabase(requireContext()).jadwalDao()
        val api = NetworkClient.getRetrofit(requireContext()).create(JadwalApi::class.java)
        JadwalViewModelFactory(JadwalRepository(d, api))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val api = NetworkClient.getRetrofit(requireContext()).create(AuthApi::class.java)
        val prefs = AppPreferences(requireContext())
        val repo = AuthRepository(api, prefs)
        viewModel = ViewModelProvider(this, AuthViewModelFactory(repo))[AuthViewModel::class.java]

        // Cek jika sudah login
        viewLifecycleOwner.lifecycleScope.launch {
            val token = prefs.tokenFlow.first()
            if (!token.isNullOrEmpty()) {
                findNavController().navigate(R.id.action_loginFragment_to_berandaFragment)
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                val authState by viewModel.authState.collectAsState()
                
                JadwalinTheme {
                    LoginScreen(
                        onLogin = { email, pass -> 
                            if (email.isNotEmpty() && pass.isNotEmpty()) {
                                viewModel.login(email, pass)
                            } else {
                                Toast.makeText(context, "Harap isi semua field", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNavigateToRegister = { findNavController().navigate(R.id.action_loginFragment_to_registerFragment) },
                        isLoading = authState is AuthState.Loading
                    )
                }

                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Success -> {
                            Toast.makeText(context, (authState as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                            jadwalVm.syncData()
                            findNavController().navigate(R.id.action_loginFragment_to_berandaFragment)
                        }
                        is AuthState.Error -> {
                            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
