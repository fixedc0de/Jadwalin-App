package com.oxygen.jadwalin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.oxygen.jadwalin.data.local.AppDatabase
import com.oxygen.jadwalin.data.remote.JadwalApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.JadwalRepository
import com.oxygen.jadwalin.databinding.FragmentBerandaBinding
import com.oxygen.jadwalin.ui.adapter.JadwalAdapter
import com.oxygen.jadwalin.ui.viewmodel.JadwalUiState
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BerandaFragment : Fragment() {
    private var _b: FragmentBerandaBinding? = null
    private val b get() = _b!!

    private val vm: JadwalViewModel by activityViewModels {
        val d = AppDatabase.getDatabase(requireContext()).jadwalDao()
        val api = NetworkClient.getRetrofit(requireContext()).create(JadwalApi::class.java)
        JadwalViewModelFactory(JadwalRepository(d, api))
    }

    private val adapter = JadwalAdapter().apply {
        showMenu = false // Sembunyikan menu titik tiga di Beranda
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentBerandaBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Trigger sinkronisasi saat membuka Beranda
        vm.syncData()

        b.rvTerdekat.layoutManager = LinearLayoutManager(requireContext())
        b.rvTerdekat.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { st ->
                    when (st) {
                        is JadwalUiState.Success -> {
                            val now = Calendar.getInstance()
                            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                            val currentIso = isoFormatter.format(now.time)
                            
                            // Hitung batas waktu akhir besok (23:59:59)
                            val endOfTomorrow = (now.clone() as Calendar).apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                            }
                            val endOfTomorrowIso = isoFormatter.format(endOfTomorrow.time)

                            // Filter: Belum lewat DAN (Hari ini ATAU Besok)
                            val filteredList = st.data.filter { 
                                it.waktu_mulai >= currentIso && it.waktu_mulai <= endOfTomorrowIso
                            }.sortedBy { it.waktu_mulai }

                            b.layoutEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
                            b.rvTerdekat.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
                            
                            adapter.submitList(filteredList)
                            
                            val todayPrefix = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                            
                            b.tvTotal.text = st.data.size.toString()
                            b.tvHariIni.text = st.data.count { it.waktu_mulai.startsWith(todayPrefix) }.toString()
                            b.tvMendatang.text = st.data.count { it.waktu_mulai > currentIso }.toString()
                        }
                        is JadwalUiState.Error -> {
                            b.layoutEmpty.visibility = View.VISIBLE
                            b.rvTerdekat.visibility = View.GONE
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
