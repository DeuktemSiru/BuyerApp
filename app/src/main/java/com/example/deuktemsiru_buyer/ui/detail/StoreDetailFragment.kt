package com.example.deuktemsiru_buyer.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartItem
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.MenuItem
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.toStore
import com.example.deuktemsiru_buyer.databinding.FragmentStoreDetailBinding
import com.example.deuktemsiru_buyer.network.CartAddRequest
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.formatPrice
import com.example.deuktemsiru_buyer.util.startTimerInto
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
    private var selectedMenuId: Long = 0
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
            try {
                val response = RetrofitClient.api.getStore(storeId).data ?: run {
                    findNavController().popBackStack()
                    return@launch
                }
                val currentIsWishlisted = isWishlisted
                val store = response.toStore(currentIsWishlisted)
                currentStore = store
                isWishlisted = currentIsWishlisted
                bindStore(store)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "가게 정보를 불러오지 못했어요.", Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
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
        binding.tvPickupRange.text = "17:00 - 18:30"
        binding.tvMenuSectionTitle.text = getString(R.string.menu_section_title, store.menus.size)

        val totalPrice = store.menus.filter { !it.isSoldOut }
            .minByOrNull { it.discountedPrice }?.discountedPrice ?: store.discountedPrice
        binding.btnReserve.text = "${totalPrice.formatPrice()} 예약하기"

        setupMenuList(store)
        startTimer(store.minutesUntilClose)
        updateCartBadge()
        updateWishlistButtons()

        val wishlistToggle = View.OnClickListener { toggleWishlist(store) }
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
                    Bundle().apply {
                        putLong("storeId", store.id)
                        putInt("totalPrice", totalPrice)
                        putLong("menuId", selectedMenuId)
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
                        if (session.isLoggedIn()) runCatching { RetrofitClient.api.clearCart() }
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
        if (session.isLoggedIn() && !syncCartAdd(item.menuId)) return
        CartManager.add(store.id, store.name, store.emoji, store.latitude, store.longitude, item)
        updateCartBadge()
        Snackbar.make(binding.root, "${item.menuName}을(를) 장바구니에 담았어요", Snackbar.LENGTH_SHORT).show()
    }

    private suspend fun syncCartAdd(productId: Long): Boolean {
        if (!session.isLoggedIn()) return true
        return runCatching {
            val item = RetrofitClient.api.addToCart(CartAddRequest(productId = productId, quantity = 1)).data
            if (item != null) {
                CartManager.addServerCartItemId(item.productId, item.cartItemId)
            }
            item != null
        }.getOrElse {
            Snackbar.make(binding.root, "서버 장바구니 동기화에 실패했어요.", Snackbar.LENGTH_SHORT).show()
            false
        }
    }

    private fun updateCartBadge() {
        if (_binding == null) return
        val count = CartManager.totalCount
        binding.tvCartBadge.isVisible = count > 0
        if (count > 0) binding.tvCartBadge.text = if (count > 9) "9+" else count.toString()
    }

    private fun toggleWishlist(store: Store) {
        if (!session.isLoggedIn()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                isWishlisted = RetrofitClient.api
                    .toggleWishlist(store.id)
                    .data
                    ?.isWishlisted
                    ?: !isWishlisted
                updateWishlistButtons()
                val msg = if (isWishlisted) "찜 목록에 추가했어요 💝" else "찜 목록에서 제거했어요"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "찜 처리 중 오류가 발생했어요.", Snackbar.LENGTH_SHORT).show()
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
                if (menu.isSoldOut) {
                    Snackbar.make(binding.root, "품절된 메뉴예요.", Snackbar.LENGTH_SHORT).show()
                } else {
                    selectedMenuId = menu.id
                    binding.btnReserve.text = "${menu.discountedPrice.formatPrice()} 예약하기"
                    Snackbar.make(binding.root, "${menu.name} 선택", Snackbar.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvMenus.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }

    private fun startTimer(minutes: Int) {
        timerJob = startTimerInto(minutes * 60L, timerJob) { remaining ->
            if (_binding == null) return@startTimerInto
            val mins = remaining / 60
            val secs = remaining % 60
            binding.tvTimer.text = "%02d:%02d".format(mins, secs)
        }
    }

    override fun onDestroyView() {
        timerJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
