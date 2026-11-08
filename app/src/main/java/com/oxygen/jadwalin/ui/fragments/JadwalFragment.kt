package com.oxygen.jadwalin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.oxygen.jadwalin.data.local.AppDatabase
import com.oxygen.jadwalin.data.remote.JadwalApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.JadwalRepository
import com.oxygen.jadwalin.ui.compose.JadwalScreen
import com.oxygen.jadwalin.ui.compose.JadwalinTheme
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModelFactory

class JadwalFragment : Fragment() {

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
        return ComposeView(requireContext()).apply {
            setContent {
                JadwalinTheme {
                    JadwalScreen(
                        vm = vm,
                        onEditJadwal = { item ->
                            (activity as? com.oxygen.jadwalin.MainActivity)?.showEditDialog(item)
                        }
                    )
                }
            }
        }
    }
}
