package com.example.deuktemsiru_buyer.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.BuildConfig
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentOnboardingBinding
import com.example.deuktemsiru_buyer.network.DebugLoginRequest
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

        if (BuildConfig.DEBUG) {
            binding.btnKakaoLogin.text = getString(R.string.login_debug_start)
        }

        binding.btnKakaoLogin.setOnClickListener {
            if (BuildConfig.DEBUG) debugLogin(session) else startKakaoLogin(session)
        }
    }

    private fun debugLogin(session: SessionManager) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.debugLogin(DebugLoginRequest())
                val loginData = response.data
                if (loginData == null) {
                    val errorMsg = response.message.ifBlank { getString(R.string.login_debug_failed) }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    return@launch
                }

                session.saveLogin(loginData)
                navigateHome()
            } catch (e: Exception) {
                val detailedError = getString(R.string.login_debug_server_error, e.localizedMessage ?: e.message ?: "Unknown Error")
                Toast.makeText(requireContext(), detailedError, Toast.LENGTH_LONG).show()
                setLoading(false)
            }
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
            Toast.makeText(requireContext(), R.string.login_kakao_failed, Toast.LENGTH_SHORT).show()
        }
        setLoading(false)
    }

    private fun loginToBackend(kakaoAccessToken: String, session: SessionManager) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loginData = RetrofitClient.api.kakaoLogin(
                    KakaoLoginRequest(kakaoAccessToken = kakaoAccessToken, role = "CONSUMER")
                ).data

                if (loginData == null) {
                    Toast.makeText(requireContext(), R.string.login_failed, Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    return@launch
                }

                session.saveLogin(loginData)
                navigateHome()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.login_server_error, Toast.LENGTH_LONG).show()
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
        binding.btnKakaoLogin.text = when {
            loading -> getString(R.string.login_processing)
            BuildConfig.DEBUG -> getString(R.string.login_debug_start)
            else -> getString(R.string.login_kakao_start)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
