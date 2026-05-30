package com.example.deuktemsiru_buyer.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartItem
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.CartRepository
import com.example.deuktemsiru_buyer.data.MenuItem
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.StoreRepository
import com.example.deuktemsiru_buyer.databinding.FragmentStoreDetailBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.Result
import com.example.deuktemsiru_buyer.util.formatPrice
import com.example.deuktemsiru_buyer.util.startCountdown
import com.example.deuktemsiru_buyer.util.toHourMinute
import com.example.deuktemsiru_buyer.util.updateCartBadge
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StoreDetailFragment : Fragment() {

    private var _binding: FragmentStoreDetailBinding? = null
    private val binding get() = _binding!!

    private var timerJob: Job? = null
    private var currentStore: Store? = null
    private var menuAdapter: MenuAdapter? = null
    private var selectedMenuId: Long = 0
    private var isWishlisted = false
    private lateinit var session: SessionManager
    private lateinit var cartRepository: CartRepository
    private val storeRepository by lazy { StoreRepository(RetrofitClient.api) }

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
        cartRepository = CartRepository(RetrofitClient.api, session)
        val storeId = arguments?.getLong("storeId") ?: 0L
        if (storeId <= 0L) {
            Snackbar.make(binding.root, "가게 정보를 확인할 수 없어요.", Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnShare.setOnClickListener {
            val storeId = arguments?.getLong("storeId") ?: return@setOnClickListener
            val shareText = "득템시루에서 ${currentStore?.name ?: "가게"}를 확인해보세요! (storeId=$storeId)"
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("가게 공유", shareText))
            Snackbar.make(binding.root, "링크가 복사되었어요.", Snackbar.LENGTH_SHORT).show()
        }

        loadStore(storeId)
    }

    private fun loadStore(storeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = storeRepository.getStore(storeId)) {
                is Result.Success -> {
                    val store = result.data
                    currentStore = store
                    isWishlisted = store.isWishlisted
                    bindStore(store)
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, "가게 정보를 불러오지 못했어요.", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun bindStore(store: Store) {
        selectedMenuId = store.menus.firstOrNull { !it.isSoldOut }?.id ?: 0
        binding.tvStoreName.text = store.name
        binding.tvRating.text = store.rating.toString()
        binding.tvWalk.text = "도보 ${store.walkingMinutes}분"
        binding.tvAddress.text = store.address
        binding.tvPhone.text = store.phone
        binding.tvPickupRange.text = pickupRangeLabel(store.menus)
        binding.tvMenuSectionTitle.text = getString(R.string.menu_section_title, store.menus.size)

        binding.btnReserve.text = selectedAvailableMenu(store)?.let { menu ->
            "${menu.discountedPrice.formatPrice()} 예약하기"
        } ?: "${store.discountedPrice.formatPrice()} 예약하기"

        setupMenuList(store)
        startTimer(store.minutesUntilClose)
        updateCartBadge()
        updateWishlistButtons()

        val wishlistToggle = View.OnClickListener { toggleWishlist(store) }
        binding.btnWishlist.setOnClickListener(wishlistToggle)
        binding.btnWishlistBottom.setOnClickListener(wishlistToggle)

        binding.btnCart.setOnClickListener {
            val menu = selectedAvailableMenu(store) ?: run {
                Snackbar.make(binding.root, "담을 수 있는 메뉴가 없어요.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addToCart(store, menu)
        }

        val allSoldOut = store.menus.all { it.isSoldOut }
        if (allSoldOut) {
            binding.btnReserve.text = "알림 신청"
            binding.btnReserve.setBackgroundResource(R.drawable.bg_surface_card)
        }

        binding.btnReserve.setOnClickListener {
            if (!allSoldOut) {
                val menu = selectedAvailableMenu(store) ?: return@setOnClickListener
                findNavController().navigate(
                    R.id.action_storeDetail_to_payment,
                    Bundle().apply {
                        putLong("storeId", store.id)
                        putInt("totalPrice", menu.discountedPrice)
                        putLong("menuId", menu.id)
                    }
                )
            } else {
                Snackbar.make(binding.root, "다음 입고 시 알림을 보내드릴게요!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun addToCart(store: Store, menu: MenuItem) {
        val item = CartItem(
            menuId = menu.id,
            menuName = menu.name,
            emoji = menu.emoji,
            originalPrice = menu.originalPrice,
            discountedPrice = menu.discountedPrice,
            pickupStart = menu.pickupStart,
            pickupEnd = menu.pickupEnd,
        )
        if (CartManager.storeId != 0L && CartManager.storeId != store.id) {
            AlertDialog.Builder(requireContext())
                .setTitle("다른 가게 메뉴가 있어요")
                .setMessage("장바구니에 ${CartManager.storeName}의 메뉴가 담겨있어요.\n비우고 ${store.name} 메뉴를 담을까요?")
                .setPositiveButton("비우고 담기") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        cartRepository.clearServerCart()
                        CartManager.clear()
                        addSyncedToCart(store, item)
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                addSyncedToCart(store, item)
            }
        }
    }

    private suspend fun addSyncedToCart(store: Store, item: CartItem) {
        setCartButtonLoading(true)
        try {
            if (session.isLoggedIn() && !syncCartAdd(item.menuId)) return
            CartManager.add(store.id, store.name, store.emoji, store.latitude, store.longitude, item)
            updateCartBadge()
            Snackbar.make(binding.root, "${item.menuName}을(를) 장바구니에 담았어요", Snackbar.LENGTH_SHORT)
                .setAction("보기") { findNavController().navigate(R.id.action_storeDetail_to_cart) }
                .show()
        } finally {
            setCartButtonLoading(false)
        }
    }

    private suspend fun syncCartAdd(productId: Long): Boolean {
        val synced = cartRepository.addProduct(productId)
        if (!synced) {
            Snackbar.make(binding.root, "서버 장바구니 동기화에 실패했어요.", Snackbar.LENGTH_SHORT).show()
        }
        return synced
    }

    private fun updateCartBadge() {
        if (_binding == null) return
        binding.tvCartBadge.updateCartBadge()
    }

    private fun setCartButtonLoading(loading: Boolean) {
        if (_binding == null) return
        binding.btnCart.isEnabled = !loading
        binding.btnCart.alpha = if (loading) 0.5f else 1.0f
        binding.btnCart.contentDescription = if (loading) "장바구니에 담는 중" else "장바구니 담기"
    }

    private fun toggleWishlist(store: Store) {
        if (!session.isLoggedIn()) return
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = storeRepository.toggleWishlist(store.id)) {
                is Result.Success -> {
                    isWishlisted = result.data
                    updateWishlistButtons()
                    val msg = if (isWishlisted) "찜 목록에 추가했어요 💝" else "찜 목록에서 제거했어요"
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, "찜 처리 중 오류가 발생했어요.", Snackbar.LENGTH_SHORT).show()
                }
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
            selectedMenuId = selectedMenuId,
            onMenuClick = { menu ->
                if (menu.isSoldOut) {
                    Snackbar.make(binding.root, "품절된 메뉴예요.", Snackbar.LENGTH_SHORT).show()
                } else {
                    selectedMenuId = menu.id
                    menuAdapter?.selectMenu(menu.id)
                    binding.btnReserve.text = "${menu.discountedPrice.formatPrice()} 예약하기"
                    Snackbar.make(binding.root, "${menu.name} 선택", Snackbar.LENGTH_SHORT).show()
                }
            }
        )
        menuAdapter = adapter
        binding.rvMenus.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }

    private fun selectedAvailableMenu(store: Store): MenuItem? =
        store.menus.firstOrNull { it.id == selectedMenuId && !it.isSoldOut }
            ?: store.menus.firstOrNull { !it.isSoldOut }

    /** 가게 전체가 픽업 가능한 구간(가장 이른 시작 ~ 가장 늦은 마감). 결제 화면 슬롯은 선택한 메뉴 기준이라 이보다 좁을 수 있다. */
    private fun pickupRangeLabel(menus: List<MenuItem>): String {
        val start = menus.mapNotNull { it.pickupStart.takeIf(String::isNotBlank) }.minOrNull()
        val end = menus.mapNotNull { it.pickupEnd.takeIf(String::isNotBlank) }.maxOrNull()
        return if (start != null && end != null) "${start.toHourMinute()} - ${end.toHourMinute()}"
        else "시간 확인 중"
    }

    private fun startTimer(minutes: Int) {
        timerJob = startCountdown(minutes * 60L, timerJob) { _binding?.tvTimer?.text = it }
    }

    override fun onDestroyView() {
        timerJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
