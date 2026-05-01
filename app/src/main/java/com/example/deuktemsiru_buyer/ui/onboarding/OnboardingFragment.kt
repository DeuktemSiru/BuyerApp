package com.example.deuktemsiru_buyer.ui.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentOnboardingBinding
import com.example.deuktemsiru_buyer.network.LoginRequest
import com.example.deuktemsiru_buyer.network.RegisterRequest
import com.example.deuktemsiru_buyer.network.RetrofitClient
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private var isRegisterMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        if (session.isLoggedIn()) {
            RetrofitClient.authToken = session.token
            findNavController().navigate(R.id.action_onboarding_to_home)
            return
        }

        updateStartButton()

        binding.cbAll.setOnCheckedChangeListener { _, isChecked ->
            binding.cbTerms.isChecked = isChecked
            binding.cbPrivacy.isChecked = isChecked
            updateStartButton()
        }

        binding.cbTerms.setOnCheckedChangeListener { _, _ ->
            updateAllCheckbox()
            updateStartButton()
        }

        binding.cbPrivacy.setOnCheckedChangeListener { _, _ ->
            updateAllCheckbox()
        }

        binding.tvTermsDetail.setOnClickListener {
            Toast.makeText(requireContext(), "이용약관 전문입니다.", Toast.LENGTH_SHORT).show()
        }

        binding.tvPrivacyDetail.setOnClickListener {
            Toast.makeText(requireContext(), "개인정보 처리방침 전문입니다.", Toast.LENGTH_SHORT).show()
        }

        binding.tvModeToggle.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateAuthMode()
            updateStartButton()
        }

        listOf(binding.etEmail, binding.etPassword, binding.etNickname).forEach { input ->
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateStartButton()
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }

        binding.btnStart.setOnClickListener {
            if (binding.cbTerms.isChecked) {
                submitAuth(session)
            }
        }

        updateAuthMode()
    }

    private fun submitAuth(session: SessionManager) {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val nickname = binding.etNickname.text?.toString()?.trim().orEmpty()

        if (email.isBlank() || password.isBlank() || (isRegisterMode && nickname.isBlank())) {
            Toast.makeText(requireContext(), "필수 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnStart.isEnabled = false
        binding.btnStart.text = if (isRegisterMode) "가입 중..." else "로그인 중..."

        lifecycleScope.launch {
            try {
                if (isRegisterMode) {
                    RetrofitClient.api.register(RegisterRequest(email, nickname, password))
                }

                val response = RetrofitClient.api.login(LoginRequest(email, password))
                if (response.role != "BUYER") {
                    Toast.makeText(requireContext(), "구매자 계정으로 로그인해주세요.", Toast.LENGTH_SHORT).show()
                    resetSubmitButton()
                    return@launch
                }
                session.userId = response.userId
                session.nickname = response.nickname
                session.token = response.token
                RetrofitClient.authToken = response.token
                findNavController().navigate(R.id.action_onboarding_to_home)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "인증에 실패했어요. 입력값과 서버 상태를 확인해주세요.", Toast.LENGTH_LONG).show()
                resetSubmitButton()
            }
        }
    }

    private fun updateAllCheckbox() {
        val allChecked = binding.cbTerms.isChecked && binding.cbPrivacy.isChecked
        binding.cbAll.setOnCheckedChangeListener(null)
        binding.cbAll.isChecked = allChecked
        binding.cbAll.setOnCheckedChangeListener { _, isChecked ->
            binding.cbTerms.isChecked = isChecked
            binding.cbPrivacy.isChecked = isChecked
            updateStartButton()
        }
    }

    private fun updateStartButton() {
        val emailReady = binding.etEmail.text?.toString()?.trim()?.isNotEmpty() == true
        val passwordReady = binding.etPassword.text?.toString()?.isNotEmpty() == true
        val nicknameReady = !isRegisterMode || binding.etNickname.text?.toString()?.trim()?.isNotEmpty() == true
        val enabled = binding.cbTerms.isChecked && emailReady && passwordReady && nicknameReady
        binding.btnStart.alpha = if (enabled) 1.0f else 0.4f
        binding.btnStart.isEnabled = enabled
    }

    private fun updateAuthMode() {
        binding.etNickname.visibility = if (isRegisterMode) View.VISIBLE else View.GONE
        binding.btnStart.text = if (isRegisterMode) "회원가입하고 시작하기" else "로그인하기"
        binding.tvModeToggle.text = if (isRegisterMode) "이미 계정이 있어요" else "처음이라면 회원가입"
    }

    private fun resetSubmitButton() {
        binding.btnStart.isEnabled = true
        binding.btnStart.text = if (isRegisterMode) "회원가입하고 시작하기" else "로그인하기"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
