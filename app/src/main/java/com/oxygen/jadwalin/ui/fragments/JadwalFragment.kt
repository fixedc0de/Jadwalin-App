package com.oxygen.jadwalin.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.oxygen.jadwalin.R
import com.oxygen.jadwalin.data.local.AppDatabase
import com.oxygen.jadwalin.data.local.JadwalEntity
import com.oxygen.jadwalin.data.remote.JadwalApi
import com.oxygen.jadwalin.data.remote.NetworkClient
import com.oxygen.jadwalin.data.repository.JadwalRepository
import com.oxygen.jadwalin.databinding.DialogEditJadwalBinding
import com.oxygen.jadwalin.databinding.FragmentJadwalBinding
import com.oxygen.jadwalin.ui.adapter.JadwalAdapter
import com.oxygen.jadwalin.ui.viewmodel.JadwalUiState
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModel
import com.oxygen.jadwalin.ui.viewmodel.JadwalViewModelFactory
import com.oxygen.jadwalin.util.AlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class JadwalFragment : Fragment() {

    private var _b: FragmentJadwalBinding? = null
    private val b get() = _b!!

    private val vm: JadwalViewModel by activityViewModels {
        val d = AppDatabase.getDatabase(requireContext()).jadwalDao()
        val api = NetworkClient.getRetrofit(requireContext()).create(JadwalApi::class.java)
        JadwalViewModelFactory(JadwalRepository(d, api))
    }

    private val adapter = JadwalAdapter()
    private var all = listOf<JadwalEntity>()
    private var filter = "Semua"
    private var search = ""
    private var sortBy = "WaktuTerdekat" // Default sort

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentJadwalBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.rvJadwal.layoutManager = LinearLayoutManager(requireContext())
        b.rvJadwal.adapter = adapter

        // 1. SETUP ACTIONS (Edit & Hapus dari Menu Titik Tiga)
        adapter.setOnActionsListener(
            edit = { jadwal -> tampilkanDialogEdit(jadwal) },
            hapus = { jadwal -> konfirmasiHapus(jadwal) }
        )

        // 2. Search & Filter Logic
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                search = s.toString().trim()
                apply()
            }
        })

        b.chipGroup.setOnCheckedStateChangeListener { _, ids ->
            filter = when (ids.firstOrNull()) {
                R.id.chipKuliah -> "Kuliah"
                R.id.chipSekolah -> "Sekolah"
                R.id.chipKerja -> "Kerja"
                R.id.chipLainnya -> "Lainnya"
                else -> "Semua"
            }
            apply()
        }

        // 2.1. Sort Action
        b.btnSort.setOnClickListener { showSortDialog() }

        // 3. Observasi Data
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { st ->
                    if (st is JadwalUiState.Success) {
                        all = st.data
                        apply()
                    }
                }
            }
        }
    }

    private fun konfirmasiHapus(jadwal: JadwalEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Jadwal?")
            .setMessage("Apakah Anda yakin ingin menghapus '${jadwal.judul}'?")
            .setPositiveButton("Hapus") { _, _ ->
                vm.hapusJadwal(jadwal)
                AlarmScheduler.cancelAlarm(requireContext(), jadwal.id)
                Snackbar.make(requireView(), "Jadwal dihapus", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf("Waktu Terdekat", "Waktu Terjauh", "Nama (A-Z)")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Urutkan Berdasarkan")
            .setItems(options) { _, which ->
                sortBy = when (which) {
                    0 -> "WaktuTerdekat"
                    1 -> "WaktuTerjauh"
                    2 -> "NamaAZ"
                    else -> "WaktuTerdekat"
                }
                apply()
            }
            .show()
    }

    private fun apply() {
        val f = all.filter {
            (filter == "Semua" || it.kategori == filter) &&
                    (search.isEmpty() || it.judul.contains(search, true))
        }.let { filtered ->
            when (sortBy) {
                "WaktuTerdekat" -> filtered.sortedBy { it.waktu_mulai }
                "WaktuTerjauh" -> filtered.sortedByDescending { it.waktu_mulai }
                "NamaAZ" -> filtered.sortedBy { it.judul.lowercase() }
                else -> filtered.sortedBy { it.waktu_mulai }
            }
        }

        b.tvEmpty.visibility = if (f.isEmpty()) View.VISIBLE else View.GONE
        b.rvJadwal.visibility = if (f.isEmpty()) View.GONE else View.VISIBLE

        val transition = AutoTransition().apply { duration = 300 }
        TransitionManager.beginDelayedTransition(b.root, transition)

        adapter.submitList(f)
    }

    private fun tampilkanDialogEdit(jadwal: JadwalEntity) {
        val dialog = BottomSheetDialog(requireContext(), R.style.Theme_Jadwalin_BottomSheetDialog)
        val db = DialogEditJadwalBinding.inflate(layoutInflater)
        dialog.setContentView(db.root)

        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED

        // Pre-fill data
        db.etJudul.setText(jadwal.judul)
        db.tvKategoriLabel.text = jadwal.kategori
        db.swTunda.isChecked = jadwal.menitSebelumnya > 0

        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val calWaktu = Calendar.getInstance().apply {
            val date = isoFormatter.parse(jadwal.waktu_mulai)
            if (date != null) time = date
        }
        db.tpWaktu.hour = calWaktu.get(Calendar.HOUR_OF_DAY)
        db.tpWaktu.minute = calWaktu.get(Calendar.MINUTE)

        var selectedKategori = jadwal.kategori
        var selectedHari = jadwal.hariDalamMinggu
        val selectedDate = calWaktu.clone() as Calendar

        fun updateTanggalVisibility() {
            val isLainnya = selectedKategori == "Lainnya"
            db.rowTanggal.visibility = if (isLainnya) View.VISIBLE else View.GONE
            db.dividerTanggal.visibility = if (isLainnya) View.VISIBLE else View.GONE
            if (isLainnya) {
                db.tvTanggalLabel.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate.time)
            }
        }
        updateTanggalVisibility()

        val ulangiItems = arrayOf("Jangan Pernah", "Setiap Minggu", "Setiap Senin", "Setiap Selasa", "Setiap Rabu", "Setiap Kamis", "Setiap Jumat", "Setiap Sabtu")
        db.tvUlangiLabel.text = if (selectedHari == -1) "Jangan Pernah" else ulangiItems[selectedHari + 1]

        // Click Listeners
        db.rowKategori.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pilih Kategori")
                .setItems(arrayOf("Kuliah", "Sekolah", "Kerja", "Lainnya")) { _, which ->
                    selectedKategori = arrayOf("Kuliah", "Sekolah", "Kerja", "Lainnya")[which]
                    db.tvKategoriLabel.text = selectedKategori
                    updateTanggalVisibility()
                }.show()
        }

        db.rowTanggal.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate.set(y, m, d)
                db.tvTanggalLabel.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate.time)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        db.rowUlangi.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ulangi")
                .setItems(ulangiItems) { _, which ->
                    selectedHari = which - 1
                    db.tvUlangiLabel.text = ulangiItems[which]
                }.show()
        }

        db.btnSimpan.setOnClickListener {
            val judulBaru = db.etJudul.text.toString().trim().ifEmpty { jadwal.judul }
            val cal = Calendar.getInstance().apply {
                if (selectedKategori == "Lainnya") {
                    set(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))
                }
                set(Calendar.HOUR_OF_DAY, db.tpWaktu.hour)
                set(Calendar.MINUTE, db.tpWaktu.minute)
                set(Calendar.SECOND, 0)
                
                if (selectedHari != -1) {
                    val targetDay = selectedHari + 1
                    while (get(Calendar.DAY_OF_WEEK) != targetDay) add(Calendar.DAY_OF_YEAR, 1)
                    if (timeInMillis < System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
                } else if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val isoString = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.time)
            val updated = jadwal.copy(
                judul = judulBaru, kategori = selectedKategori, waktu_mulai = isoString,
                isRecurring = selectedHari != -1, hariDalamMinggu = selectedHari,
                menitSebelumnya = if (db.swTunda.isChecked) 30 else 0
            )

            vm.updateJadwal(updated)
            AlarmScheduler.cancelAlarm(requireContext(), jadwal.id)
            
            lifecycleScope.launch {
                val menit = com.oxygen.jadwalin.data.local.AppPreferences(requireContext()).reminderMinutesFlow.first()
                AlarmScheduler.scheduleAlarm(requireContext(), jadwal.id, judulBaru, isoString, if (db.swTunda.isChecked) menit else 0, updated.isRecurring)
            }

            Toast.makeText(requireContext(), "Jadwal diperbarui!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        db.btnBatal.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
