package com.example.deuktemsiru_buyer.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.databinding.FragmentSiruLinkBinding

class SiruLinkFragment : Fragment() {

    private var _binding: FragmentSiruLinkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSiruLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLinkSiru.setOnClickListener {
            Toast.makeText(requireContext(), "목업 시루 계정이 연동됐어요.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_siru_to_home)
        }

        binding.btnSkip.setOnClickListener {
            findNavController().navigate(R.id.action_siru_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
