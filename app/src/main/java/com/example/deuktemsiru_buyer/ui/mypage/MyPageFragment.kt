package com.example.deuktemsiru_buyer.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentMypageBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import kotlinx.coroutines.launch

class MyPageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        if (session.isLoggedIn()) {
            loadUser()
        }

        binding.menuSiruPayment.setOnClickListener {
            showSiruPaymentInfo()
        }
        binding.menuFavoriteStores.setOnClickListener {
            showFavoriteStores()
        }
        binding.menuSettings.setOnClickListener {
            showSettings()
        }
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val user = RetrofitClient.api.getMe().data ?: return@launch
                val stats = RetrofitClient.api.getMyStats().data

                binding.tvNickname.text = user.nickname
                binding.tvCarbonTotal.text = "%.1f".format(stats?.totalCarbonSavedKg ?: 0.0)
                binding.tvCarbonSavedCount.text = "총 ${stats?.totalOrders ?: 0}개의 음식을 구출하셨어요!"
                binding.tvEcoLevel.text = "에코 레벨: ${gradeLabel(stats?.totalOrders ?: 0)}"
                binding.tvEcoNext.text = nextGradeHint(stats?.totalOrders ?: 0)
                binding.tvCouponCount.text = "0"
                binding.tvPoints.text = "%,dP".format((stats?.totalSavedAmount ?: 0) / 10)
                SessionManager(requireContext()).apply {
                    isSiruLinked = user.isSiruLinked
                    siruBalance = user.siruBalance
                }

                val progressRatio = ((stats?.totalOrders ?: 0) / 10f).coerceIn(0.2f, 1.0f)
                binding.progressEco.post {
                    val parentWidth = (binding.progressEco.parent as View).width
                    binding.progressEco.layoutParams.width = (parentWidth * progressRatio).toInt()
                    binding.progressEco.requestLayout()
                }
            } catch (e: Exception) {
                // 오류 시 기존 텍스트 유지
            }
        }
    }

    private fun showSiruPaymentInfo() {
        AlertDialog.Builder(requireContext())
            .setTitle("시루 결제 관리")
            .setMessage(
                if (SessionManager(requireContext()).isSiruLinked)
                    "현재 시루 잔액은 %,d원입니다.\n\n시루 결제 시 5% 추가 인센티브가 적용됩니다."
                        .format(SessionManager(requireContext()).siruBalance)
                else
                    "시루 계정이 아직 연동되지 않았어요.\n\n시루 결제 시 5% 추가 인센티브가 적용됩니다."
            )
            .setPositiveButton("확인", null)
            .show()
    }

    private fun showFavoriteStores() {
        findNavController().navigate(R.id.wishlistFragment)
    }

    private fun showSettings() {
        lifecycleScope.launch {
            try {
                val settings = RetrofitClient.api.getNotificationSettings().data
                val labels = arrayOf("새 상품 알림", "픽업 리마인드", "주문 상태 알림", "이벤트 알림")
                val checked = booleanArrayOf(
                    settings?.newProduct ?: true,
                    settings?.pickupReminder ?: true,
                    settings?.orderConfirmed ?: true,
                    settings?.event ?: true,
                )
                AlertDialog.Builder(requireContext())
                    .setTitle("알림 설정")
                    .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
                    .setPositiveButton("저장") { _, _ ->
                        lifecycleScope.launch {
                            RetrofitClient.api.updateNotificationSettings(
                                com.example.deuktemsiru_buyer.network.UpdateNotificationSettingsRequest(
                                    newProduct = checked[0],
                                    pickupReminder = checked[1],
                                    orderConfirmed = checked[2],
                                    event = checked[3],
                                )
                            )
                            Toast.makeText(requireContext(), "알림 설정을 저장했어요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "설정을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun gradeLabel(totalOrders: Int) = when {
        totalOrders >= 30 -> "숲"
        totalOrders >= 15 -> "나무"
        totalOrders >= 5 -> "새싹+"
        else -> "새싹"
    }

    private fun nextGradeHint(totalOrders: Int) = when {
        totalOrders < 5 -> "5회 주문하면 새싹+로 성장해요"
        totalOrders < 15 -> "15회 주문하면 나무로 성장해요"
        totalOrders < 30 -> "30회 주문하면 숲으로 성장해요"
        else -> "최고 등급이에요!"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
