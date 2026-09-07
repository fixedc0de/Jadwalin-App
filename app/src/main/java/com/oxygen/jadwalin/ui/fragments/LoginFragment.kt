package com.oxygen.jadwalin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.oxygen.jadwalin.R
import com.oxygen.jadwalin.data.local.AppPreferences
import com.oxygen.jadwalin.data.remote.AuthApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.AuthRepository
import com.oxygen.jadwalin.databinding.FragmentLoginBinding
import com.oxygen.jadwalin.ui.viewmodel.AuthState
import com.oxygen.jadwalin.ui.viewmodel.AuthViewModel
import com.oxygen.jadwalin.ui.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val api = NetworkClient.getRetrofit(requireContext()).create(AuthApi::class.java)
        val prefs = AppPreferences(requireContext())
        val repo = AuthRepository(api, prefs)
        viewModel = ViewModelProvider(this, AuthViewModelFactory(repo))[AuthViewModel::class.java]

        binding.tvToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.login(email, pass)
            } else {
                Toast.makeText(context, "Harap isi semua field", Toast.LENGTH_SHORT).show()
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> binding.pbLoading.visibility = View.VISIBLE
                    is AuthState.Success -> {
                        binding.pbLoading.visibility = View.GONE
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_berandaFragment)
                    }
                    is AuthState.Error -> {
                        binding.pbLoading.visibility = View.GONE
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    else -> binding.pbLoading.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}