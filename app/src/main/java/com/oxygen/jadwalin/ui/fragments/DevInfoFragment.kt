package com.oxygen.jadwalin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.oxygen.jadwalin.databinding.FragmentDevInfoBinding

class DevInfoFragment : Fragment() {
    private var _b: FragmentDevInfoBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentDevInfoBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.layoutBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
