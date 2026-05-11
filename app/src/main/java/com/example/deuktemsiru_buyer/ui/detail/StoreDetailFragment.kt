package com.example.deuktemsiru_buyer.ui.detail

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartItem
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.MenuItem
import com.example.deuktemsiru_buyer.data.SampleData
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
        savedInstanceState: Bundle?
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
                val userId = if (session.isLoggedIn()) session.userId else null
                val response = RetrofitClient.api.getStore(storeId, userId)
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
        binding.tvPickupRange.text = store.menus.firstOrNull()?.let {
            it.name + " 외"
        } ?: "메뉴"

        val totalPrice = store.menus.filter { !it.isSoldOut }
            .minByOrNull { it.discountedPrice }?.discountedPrice ?: store.discountedPrice
        binding.btnReserve.text = "${SampleData.formatPrice(totalPrice)} 예약하기"

        setupMenuList(store)
        startTimer(store.minutesUntilClose)
        updateCartBadge()

        updateWishlistButtons()

        val wishlistToggle = View.OnClickListener {
            toggleWishlist(store)
        }
        binding.btnWishlist.setOnClickListener(wishlistToggle)
        binding.btnWishlistBottom.setOnClickListener(wishlistToggle)

        binding.btnCart.setOnClickListener {
            val menu = store.menus.firstOrNull { it.id == selectedMenuId && !it.isSoldOut }
                ?: store.menus.firstOrNull { !it.isSoldOut }
                ?: return@setOnClickListener
            addToCart(store, menu)
        }

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

    private fun addToCart(store: Store, menu: MenuItem) {
        val item = CartItem(
            menuId = menu.id.toLong(),
            menuName = menu.name,
            emoji = menu.emoji,
            originalPrice = menu.originalPrice,
            discountedPrice = menu.discountedPrice,
        )
        val added = CartManager.add(store.id.toLong(), store.name, store.emoji, store.latitude, store.longitude, item)
        if (!added) {
            AlertDialog.Builder(requireContext())
                .setTitle("다른 가게 메뉴가 있어요")
                .setMessage("장바구니에 ${CartManager.storeName}의 메뉴가 담겨있어요.\n비우고 ${store.name} 메뉴를 담을까요?")
                .setPositiveButton("비우고 담기") { _, _ ->
                    CartManager.clear()
                    CartManager.add(store.id.toLong(), store.name, store.emoji, store.latitude, store.longitude, item)
                    updateCartBadge()
                    Toast.makeText(requireContext(), "${menu.name}을(를) 장바구니에 담았어요", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("취소", null)
                .show()
        } else {
            updateCartBadge()
            Toast.makeText(requireContext(), "${menu.name}을(를) 장바구니에 담았어요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCartBadge() {
        if (_binding == null) return
        val count = CartManager.totalCount
        if (count > 0) {
            binding.tvCartBadge.visibility = View.VISIBLE
            binding.tvCartBadge.text = if (count > 9) "9+" else count.toString()
        } else {
            binding.tvCartBadge.visibility = View.GONE
        }
    }

    private fun toggleWishlist(store: Store) {
        if (!session.isLoggedIn()) return
        lifecycleScope.launch {
            try {
                val result = RetrofitClient.api.toggleWishlist(store.id.toLong(), session.userId)
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

    private fun setupMenuList(store: Store) {
        val adapter = MenuAdapter(
            menus = store.menus,
            onMenuClick = { menu ->
                selectedMenuId = menu.id
                binding.btnReserve.text = "${SampleData.formatPrice(menu.discountedPrice)} 예약하기"
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
