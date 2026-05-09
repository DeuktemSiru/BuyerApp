package com.example.deuktemsiru_buyer.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentMypageBinding
import com.example.deuktemsiru_buyer.network.NotificationApiResponse
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

        binding.menuOrders.setOnClickListener {
            Toast.makeText(requireContext(), "주문 내역", Toast.LENGTH_SHORT).show()
        }
        binding.menuPlaces.setOnClickListener {
            Toast.makeText(requireContext(), "자주 가는 위치", Toast.LENGTH_SHORT).show()
        }
        binding.menuNotifications.setOnClickListener { showNotifications(session) }
        binding.menuSupport.setOnClickListener {
            Toast.makeText(requireContext(), "고객 문의", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val user = RetrofitClient.api.getMe().data ?: return@launch

                binding.tvNickname.text = user.nickname
                binding.tvGrade.text = gradeLabel(user.grade)
                binding.tvTotalSavings.text = "%,d원".format(user.totalSavings)
                binding.tvMonthSavings.text = "%,d원".format(0)
                binding.tvCarbon.text = "%.1fkg".format(user.co2Saved)
                binding.tvCouponCount.text = user.couponCount.toString()
                binding.tvPoints.text = "%,dP".format(user.points)

                val progressRatio = when (user.grade) {
                    "SEEDLING" -> 0.2f
                    "SPROUT" -> 0.5f
                    "TREE" -> 0.8f
                    "FOREST" -> 1.0f
                    else -> 0.2f
                }
                binding.tvNextGrade.text = nextGradeHint(user.grade)
                binding.progressGrade.post {
                    val parentWidth = (binding.progressGrade.parent as View).width
                    binding.progressGrade.layoutParams.width = (parentWidth * progressRatio).toInt()
                    binding.progressGrade.requestLayout()
                }
            } catch (e: Exception) {
                // 오류 시 기존 텍스트 유지
            }
        }
    }

    private fun showNotifications(session: SessionManager) {
        if (!session.isLoggedIn()) return

        lifecycleScope.launch {
            try {
                val notifications = RetrofitClient.api.getNotifications().data ?: emptyList()
                val message = if (notifications.isEmpty()) {
                    "찜한 가게에서 온 알림이 아직 없어요."
                } else {
                    notifications.joinToString("\n\n") { it.dialogLine() }
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("알림")
                    .setMessage(message)
                    .setPositiveButton("확인", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "알림을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun NotificationApiResponse.dialogLine(): String = "$storeName\n$message"

    private fun gradeLabel(grade: String) = when (grade) {
        "SEEDLING" -> "🌱 새싹"
        "SPROUT" -> "🌿 새싹+"
        "TREE" -> "🌳 나무"
        "FOREST" -> "🌲 숲"
        else -> "🌱 새싹"
    }

    private fun nextGradeHint(grade: String) = when (grade) {
        "SEEDLING" -> "더 구하면 🌿 새싹+로 성장해요"
        "SPROUT" -> "더 구하면 🌳 나무로 성장해요"
        "TREE" -> "더 구하면 🌲 숲으로 성장해요"
        else -> "최고 등급이에요! 🎉"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
