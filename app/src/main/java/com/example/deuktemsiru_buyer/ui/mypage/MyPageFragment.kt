package com.example.deuktemsiru_buyer.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentMypageBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.toast
import kotlinx.coroutines.launch

class MyPageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager

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

        session = SessionManager(requireContext())
        binding.tvNickname.text = session.nickname
        if (session.isLoggedIn()) {
            loadUser()
        }

        binding.menuSiruPayment.setOnClickListener { showSiruPaymentInfo() }
        binding.menuFavoriteStores.setOnClickListener { showFavoriteStores() }
        binding.menuSettings.setOnClickListener { showSettings() }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠어요?")
                .setPositiveButton("로그아웃") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        runCatching { RetrofitClient.api.logout() }
                        session.clear()
                        RetrofitClient.accessToken = null
                        RetrofitClient.refreshToken = null
                        findNavController().navigate(
                            R.id.onboardingFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.onboardingFragment, true)
                                .build()
                        )
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun loadUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = RetrofitClient.api.getMe().data ?: return@launch
                val stats = RetrofitClient.api.getMyStats().data
                val totalOrders = stats?.totalOrders ?: 0
                val wishlistCount = runCatching {
                    RetrofitClient.api.getWishlist().data?.wishlists?.size
                }.getOrNull() ?: 0

                binding.tvNickname.text = user.nickname
                binding.tvCarbonTotal.text = "%.1f".format(stats?.totalCarbonSavedKg ?: 0.0)
                binding.tvCarbonSavedCount.text = "총 ${totalOrders}개의 음식을 구출하셨어요!"
                binding.tvTotalSavings.text = "%,d원".format(stats?.totalSavedAmount ?: 0)
                binding.tvEcoLevel.text = "에코 레벨: ${gradeLabel(stats?.grade, totalOrders)}"
                binding.tvEcoNext.text = nextGradeHint(stats?.grade, totalOrders)
                binding.tvCouponCount.text = (stats?.couponCount ?: 0).toString()
                binding.tvPoints.text = "%,dP".format(stats?.points ?: 0)
                binding.tvWishlistCount.text = wishlistCount.toString()
                session.nickname = user.nickname
                session.isSiruLinked = user.isSiruLinked
                session.siruBalance = user.siruBalance

                val progressRatio = gradeProgress(stats?.grade, totalOrders)
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
                if (session.isSiruLinked)
                    "현재 시루 잔액은 %,d원입니다.\n\n시루 결제 시 5% 추가 인센티브가 적용됩니다.".format(session.siruBalance)
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
        viewLifecycleOwner.lifecycleScope.launch {
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
                        viewLifecycleOwner.lifecycleScope.launch {
                            RetrofitClient.api.updateNotificationSettings(
                                com.example.deuktemsiru_buyer.network.UpdateNotificationSettingsRequest(
                                    newProduct = checked[0],
                                    pickupReminder = checked[1],
                                    orderConfirmed = checked[2],
                                    event = checked[3],
                                )
                            )
                            toast("알림 설정을 저장했어요.")
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            } catch (e: Exception) {
                toast("설정을 불러오지 못했어요.")
            }
        }
    }

    private fun gradeLabel(grade: String?, totalOrders: Int) =
        apiGradeLabels[grade] ?: ecoGrades.last { totalOrders >= it.minOrders }.label

    private fun nextGradeHint(grade: String?, totalOrders: Int): String {
        val apiGradeIndex = apiGradeOrder.indexOf(grade)
        if (apiGradeIndex >= 0) {
            return apiGradeOrder.getOrNull(apiGradeIndex + 1)
                ?.let { "더 구하면 ${apiGradeLabels.getValue(it)}로 성장해요" }
                ?: "최고 등급이에요!"
        }
        return ecoGrades.firstOrNull { totalOrders < it.minOrders }
            ?.let { "${it.minOrders}회 주문하면 ${it.label}로 성장해요" }
            ?: "최고 등급이에요!"
    }

    private fun gradeProgress(grade: String?, totalOrders: Int): Float {
        val apiGradeIndex = apiGradeOrder.indexOf(grade)
        if (apiGradeIndex >= 0) return (apiGradeIndex + 1f) / apiGradeOrder.size
        val nextGrade = ecoGrades.firstOrNull { totalOrders < it.minOrders } ?: return 1f
        return (totalOrders.toFloat() / nextGrade.minOrders).coerceIn(0.2f, 1f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private data class EcoGrade(val minOrders: Int, val label: String)

private val ecoGrades = listOf(
    EcoGrade(0, "새싹"),
    EcoGrade(5, "새싹+"),
    EcoGrade(15, "나무"),
    EcoGrade(30, "숲"),
)

private val apiGradeOrder = listOf("SEEDLING", "SPROUT", "TREE", "FOREST")

private val apiGradeLabels = mapOf(
    "SEEDLING" to "새싹 🌱",
    "SPROUT" to "새싹+ 🌿",
    "TREE" to "나무 🌳",
    "FOREST" to "숲 🌲",
)
