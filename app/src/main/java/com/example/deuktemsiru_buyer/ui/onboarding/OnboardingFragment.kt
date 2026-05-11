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

        if (session.isLoggedIn()) {
            session.restoreToken()
            navigateHome()
            return
        }

        binding.btnKakaoLogin.setOnClickListener {
            startKakaoLogin(session)
        }
    }

    private fun startKakaoLogin(session: SessionManager) {
        setLoading(true)

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            when {
                error != null -> handleKakaoError(error)
                token != null -> loginToBackend(token.accessToken, session)
            }
        }

        val context = requireContext()
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    if (error.isUserCancelled()) setLoading(false)
                    else UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                } else if (token != null) {
                    loginToBackend(token.accessToken, session)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }

    private fun handleKakaoError(error: Throwable) {
        if (!error.isUserCancelled()) {
            Toast.makeText(requireContext(), "카카오 로그인에 실패했어요", Toast.LENGTH_SHORT).show()
        }
        setLoading(false)
    }

    private fun loginToBackend(kakaoAccessToken: String, session: SessionManager) {
        lifecycleScope.launch {
            try {
                val loginData = RetrofitClient.api.kakaoLogin(
                    KakaoLoginRequest(kakaoAccessToken = kakaoAccessToken, role = "CONSUMER")
                ).data

                if (loginData == null) {
                    Toast.makeText(requireContext(), "로그인에 실패했어요", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    return@launch
                }

                session.memberId = loginData.member.memberId
                session.nickname = loginData.member.nickname
                session.accessToken = loginData.accessToken
                session.refreshToken = loginData.refreshToken

                navigateHome()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "서버 로그인에 실패했어요. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    private fun Throwable.isUserCancelled() =
        this is ClientError && reason == ClientErrorCause.Cancelled

    private fun navigateHome() {
        findNavController().navigate(R.id.action_onboarding_to_home)
    }

    private fun setLoading(loading: Boolean) {
        binding.btnKakaoLogin.isEnabled = !loading
        binding.btnKakaoLogin.text = if (loading) "로그인 중..." else "카카오로 시작하기"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
