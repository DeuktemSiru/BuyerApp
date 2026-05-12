package com.example.deuktemsiru_buyer.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
            showPolicy(
                "이용약관",
                "득템시루는 마감 할인 상품 탐색과 픽업 예약을 돕는 서비스입니다.\n\n사용자는 정확한 주문 정보와 픽업 시간을 확인해야 하며, 매장과 서비스 운영을 방해하는 행위를 할 수 없습니다. 주문 취소와 환불은 매장 준비 상태와 서비스 정책에 따라 제한될 수 있습니다."
            )
        }

        binding.tvPrivacyDetail.setOnClickListener {
            showPolicy(
                "개인정보 처리방침",
                "득템시루는 로그인, 주문, 픽업 안내, 고객 문의 처리를 위해 필요한 최소한의 정보를 사용합니다.\n\n처리 항목에는 카카오 계정 식별자, 닉네임, 주문 내역, 픽업 코드, 알림 수신 내역이 포함될 수 있습니다. 위치 정보는 주변 매장과 경로 안내를 위해 사용자의 동의가 있을 때만 활용합니다."
            )
        }

        binding.tvMarketingDetail.setOnClickListener {
            showPolicy(
                "혜택 알림 수신 안내",
                "혜택 알림에 동의하면 찜한 매장의 할인 소식, 픽업 가능 상품, 이벤트 안내를 받을 수 있습니다.\n\n마케팅 알림 수신 동의는 선택 항목이며, 동의하지 않아도 기본 서비스 이용에는 제한이 없습니다."
            )
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

    private fun showPolicy(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
