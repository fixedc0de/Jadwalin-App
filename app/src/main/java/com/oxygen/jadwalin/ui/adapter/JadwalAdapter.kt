package com.oxygen.jadwalin.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oxygen.jadwalin.R
import com.oxygen.jadwalin.data.local.JadwalEntity
import com.oxygen.jadwalin.databinding.ItemJadwalBinding
import java.text.SimpleDateFormat
import java.util.Locale

class JadwalAdapter : ListAdapter<JadwalEntity, JadwalAdapter.VH>(DiffCallback) {
    private var onEdit: ((JadwalEntity) -> Unit)? = null
    private var onHapus: ((JadwalEntity) -> Unit)? = null
    
    // Flag untuk menampilkan atau menyembunyikan menu titik tiga
    var showMenu: Boolean = true

    fun setOnActionsListener(edit: (JadwalEntity) -> Unit, hapus: (JadwalEntity) -> Unit) {
        onEdit = edit
        onHapus = hapus
    }

    class VH(val b: ItemJadwalBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = 
        VH(ItemJadwalBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, p: Int) {
        val j = getItem(p)
        h.b.tvJudul.text = j.judul
        h.b.tvWaktu.text = format(j.waktu_mulai)
        h.b.tvKategori.text = j.kategori.uppercase()

        val ctx = h.itemView.context
        val (bgColor, textColor) = when (j.kategori.lowercase()) {
            "kuliah" -> R.color.cat_kuliah_bg to R.color.cat_kuliah_text
            "kerja", "proyek" -> R.color.cat_kerja_bg to R.color.cat_kerja_text
            "pribadi" -> R.color.cat_pribadi_bg to R.color.cat_pribadi_text
            else -> R.color.brand_primary to R.color.white
        }

        h.b.viewIndicator.setBackgroundColor(ContextCompat.getColor(ctx, textColor))
        
        // Update tvKategori to look like a chip
        h.b.tvKategori.setTextColor(ContextCompat.getColor(ctx, textColor))
        h.b.tvKategori.backgroundTintList = ContextCompat.getColorStateList(ctx, bgColor)

        // Kontrol visibilitas menu titik tiga
        h.b.btnMenu.visibility = if (showMenu) View.VISIBLE else View.GONE

        // Three-dot Menu Logic
        h.b.btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(ctx, view)
            popup.menu.add("Ubah")
            popup.menu.add("Hapus")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Ubah" -> onEdit?.invoke(j)
                    "Hapus" -> onHapus?.invoke(j)
                }
                true
            }
            popup.show()
        }
    }

    private fun format(iso: String) = try {
        // Parsing harus konsisten (Locale.US)
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)!!
        // Tampilan boleh pakai Locale.getDefault()
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        iso
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<JadwalEntity>() {
            override fun areItemsTheSame(oldItem: JadwalEntity, newItem: JadwalEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: JadwalEntity, newItem: JadwalEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
