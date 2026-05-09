package com.example.deuktemsiru_buyer.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.databinding.FragmentTermsBinding

class TermsFragment : Fragment() {

    private var _binding: FragmentTermsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateContinueButton()

        binding.cbAll.setOnCheckedChangeListener { _, isChecked ->
            binding.cbTerms.isChecked = isChecked
            binding.cbPrivacy.isChecked = isChecked
            binding.cbMarketing.isChecked = isChecked
            updateContinueButton()
        }

        binding.cbTerms.setOnCheckedChangeListener { _, _ ->
            updateAllCheckbox()
            updateContinueButton()
        }

        binding.cbPrivacy.setOnCheckedChangeListener { _, _ ->
            updateAllCheckbox()
            updateContinueButton()
        }

        binding.cbMarketing.setOnCheckedChangeListener { _, _ ->
            updateAllCheckbox()
        }

        binding.tvTermsDetail.setOnClickListener {
            Toast.makeText(requireContext(), "이용약관 전문입니다.", Toast.LENGTH_SHORT).show()
        }

        binding.tvPrivacyDetail.setOnClickListener {
            Toast.makeText(requireContext(), "개인정보 처리방침 전문입니다.", Toast.LENGTH_SHORT).show()
        }

        binding.tvMarketingDetail.setOnClickListener {
            Toast.makeText(requireContext(), "혜택 알림 수신 안내입니다.", Toast.LENGTH_SHORT).show()
        }

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_terms_to_siru)
        }
    }

    private fun updateAllCheckbox() {
        val allChecked = binding.cbTerms.isChecked &&
            binding.cbPrivacy.isChecked &&
            binding.cbMarketing.isChecked
        binding.cbAll.setOnCheckedChangeListener(null)
        binding.cbAll.isChecked = allChecked
        binding.cbAll.setOnCheckedChangeListener { _, isChecked ->
            binding.cbTerms.isChecked = isChecked
            binding.cbPrivacy.isChecked = isChecked
            binding.cbMarketing.isChecked = isChecked
            updateContinueButton()
        }
    }

    private fun updateContinueButton() {
        val enabled = binding.cbTerms.isChecked && binding.cbPrivacy.isChecked
        binding.btnContinue.alpha = if (enabled) 1.0f else 0.4f
        binding.btnContinue.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
