package com.example.deuktemsiru_buyer.ui.detail

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.toStore
import com.example.deuktemsiru_buyer.databinding.FragmentStoreDetailBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import kotlinx.coroutines.launch

class StoreDetailFragment : Fragment() {

    private var _binding: FragmentStoreDetailBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 0
    private var timerRunnable: Runnable? = null
    private var currentStore: Store? = null
    private var selectedMenuId: Int = 0
    private var isWishlisted = false
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStoreDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        val storeId = arguments?.getInt("storeId") ?: 1

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnShare.setOnClickListener {
            Toast.makeText(requireContext(), "링크가 복사되었어요.", Toast.LENGTH_SHORT).show()
        }

        loadStore(storeId.toLong())
    }

    private fun loadStore(storeId: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getStore(storeId).data ?: run {
                    findNavController().popBackStack()
                    return@launch
                }
                val store = response.toStore()
                currentStore = store
                isWishlisted = store.isWishlisted
                bindStore(store)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "가게 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun bindStore(store: Store) {
        selectedMenuId = store.menus.firstOrNull { !it.isSoldOut }?.id ?: 0
        binding.tvStoreName.text = store.name
        binding.tvRating.text = store.rating.toString()
        binding.tvWalk.text = "도보 ${store.walkingMinutes}분"
        binding.tvPickupRange.text = store.menus.firstOrNull()?.let { it.name + " 외" } ?: "메뉴"

        val totalPrice = store.menus.filter { !it.isSoldOut }
            .minByOrNull { it.discountedPrice }?.discountedPrice ?: store.discountedPrice
        binding.btnReserve.text = "%,d원 예약하기".format(totalPrice)

        setupMenuList(store, totalPrice)
        startTimer(store.minutesUntilClose)
        updateWishlistButtons()

        val wishlistToggle = View.OnClickListener { toggleWishlist(store) }
        binding.btnWishlist.setOnClickListener(wishlistToggle)
        binding.btnWishlistBottom.setOnClickListener(wishlistToggle)

        val allSoldOut = store.menus.all { it.isSoldOut }
        if (allSoldOut) {
            binding.btnReserve.text = "알림 신청"
            binding.btnReserve.setBackgroundResource(R.drawable.bg_surface_card)
        }

        binding.btnReserve.setOnClickListener {
            if (!allSoldOut) {
                findNavController().navigate(
                    R.id.action_storeDetail_to_payment,
                    bundleOf("storeId" to store.id, "totalPrice" to totalPrice, "menuId" to selectedMenuId)
                )
            } else {
                Toast.makeText(requireContext(), "다음 입고 시 알림을 보내드릴게요!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleWishlist(store: Store) {
        if (!session.isLoggedIn()) return
        lifecycleScope.launch {
            try {
                val result = RetrofitClient.api.toggleWishlist(store.id.toLong()).data ?: emptyMap()
                isWishlisted = result["isWishlisted"] as? Boolean ?: !isWishlisted
                updateWishlistButtons()
                val msg = if (isWishlisted) "찜 목록에 추가했어요 💝" else "찜 목록에서 제거했어요"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "찜 처리 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateWishlistButtons() {
        val res = if (isWishlisted) R.drawable.ic_heart_filled else R.drawable.ic_heart
        binding.btnWishlist.setImageResource(res)
        binding.btnWishlistBottom.setImageResource(res)
    }

    private fun setupMenuList(store: Store, defaultTotalPrice: Int) {
        val adapter = MenuAdapter(
            menus = store.menus,
            onMenuClick = { menu ->
                selectedMenuId = menu.id
                binding.btnReserve.text = "%,d원 예약하기".format(menu.discountedPrice)
                Toast.makeText(requireContext(), "${menu.name} 선택", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvMenus.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }

    private fun startTimer(minutes: Int) {
        timerRunnable?.let { handler.removeCallbacks(it) }
        remainingSeconds = minutes * 60
        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                if (remainingSeconds > 0) {
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    binding.tvTimer.text = "%02d:%02d".format(mins, secs)
                    remainingSeconds--
                    handler.postDelayed(this, 1000)
                } else {
                    binding.tvTimer.text = "00:00"
                }
            }
        }
        handler.post(timerRunnable!!)
    }

    override fun onDestroyView() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroyView()
        _binding = null
    }
}
