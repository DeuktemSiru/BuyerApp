package com.example.deuktemsiru_buyer.ui.onboarding

import android.os.Bundle
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
import com.example.deuktemsiru_buyer.network.KakaoLoginRequest
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())

        // 이미 로그인된 경우 홈으로 바로 이동
        if (session.isLoggedIn()) {
            session.restoreToken()
            findNavController().navigate(R.id.action_onboarding_to_home)
            return
        }

        binding.btnKakaoLogin.setOnClickListener {
            startKakaoLogin(session)
        }
    }

    private fun startKakaoLogin(session: SessionManager) {
        binding.btnKakaoLogin.isEnabled = false
        binding.btnKakaoLogin.text = "로그인 중..."

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "카카오 로그인에 실패했어요", Toast.LENGTH_SHORT).show()
                resetButton()
            } else if (token != null) {
                sendTokenToServer(token.accessToken, session)
            }
        }

        // 카카오톡 로그인 시도 → 실패 시 카카오 계정 로그인으로 폴백
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
            UserApiClient.instance.loginWithKakaoTalk(requireContext()) { token, error ->
                if (error != null) {
                    // 사용자가 카카오톡 로그인을 취소한 경우 폴백 하지 않음
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        resetButton()
                        return@loginWithKakaoTalk
                    }
                    // 그 외 에러 → 카카오 계정 로그인으로 폴백
                    UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
                } else if (token != null) {
                    sendTokenToServer(token.accessToken, session)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
        }
    }

    private fun sendTokenToServer(kakaoAccessToken: String, session: SessionManager) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.kakaoLogin(
                    KakaoLoginRequest(kakaoAccessToken = kakaoAccessToken, role = "BUYER")
                )
                val loginData = response.data
                if (loginData == null) {
                    Toast.makeText(requireContext(), "로그인에 실패했어요", Toast.LENGTH_SHORT).show()
                    resetButton()
                    return@launch
                }

                session.memberId = loginData.member.memberId
                session.nickname = loginData.member.nickname
                session.accessToken = loginData.accessToken
                session.refreshToken = loginData.refreshToken

                findNavController().navigate(R.id.action_onboarding_to_home)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "서버 로그인에 실패했어요. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                resetButton()
            }
        }
    }

    private fun resetButton() {
        binding.btnKakaoLogin.isEnabled = true
        binding.btnKakaoLogin.text = "카카오로 시작하기"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
