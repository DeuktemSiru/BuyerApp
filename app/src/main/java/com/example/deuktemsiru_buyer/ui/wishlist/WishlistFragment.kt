package com.example.deuktemsiru_buyer.ui.wishlist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.categoryToApi
import com.example.deuktemsiru_buyer.data.toStore
import com.example.deuktemsiru_buyer.databinding.FragmentWishlistBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.ui.home.StoreAdapter
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    private val allStores = mutableListOf<Store>()
    private var currentCategory = "전체"
    private lateinit var adapter: StoreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupCategoryChips()

        val session = SessionManager(requireContext())
        if (!session.isLoggedIn()) {
            binding.progress.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val stores = RetrofitClient.api.getWishlist().data?.wishlists
                    ?.map { item -> item.toStore() }
                    ?: emptyList()
                allStores.clear()
                allStores.addAll(stores)
                binding.progress.visibility = View.GONE
                updateList(filterStores())
            } catch (e: Exception) {
                binding.progress.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "찜 목록을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StoreAdapter(
            onStoreClick = { store ->
                findNavController().navigate(
                    R.id.action_wishlist_to_storeDetail,
                    Bundle().apply { putInt("storeId", store.id) }
                )
            },
            onWishlistClick = { store ->
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        RetrofitClient.api.toggleWishlist(store.id.toLong())
                        allStores.removeAll { it.id == store.id }
                        updateList(filterStores())
                        Toast.makeText(requireContext(), "찜 목록에서 제거했어요", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(requireContext(), "찜 처리 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvWishlist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@WishlistFragment.adapter
        }
    }

    private fun setupCategoryChips() {
        val chips = mapOf(
            binding.chipAll to "전체",
            binding.chipKorean to "한식",
            binding.chipWestern to "양식",
            binding.chipCafeDessert to "카페·디저트",
            binding.chipBakery to "베이커리",
            binding.chipCafe to "카페"
        )

        chips.forEach { (chip, category) ->
            chip.setOnClickListener {
                currentCategory = category
                chips.forEach { (c, _) ->
                    c.setBackgroundResource(R.drawable.bg_chip_unselected)
                    c.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text))
                }
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnPrimary))

                updateList(filterStores())
            }
        }
    }

    private fun setupSearch() {
        binding.etWishlistSearch.doOnTextChanged { _, _, _, _ ->
            updateList(filterStores())
        }
        binding.etWishlistSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                updateList(filterStores())
                true
            } else {
                false
            }
        }
        binding.btnWishlistSearch.setOnClickListener {
            hideKeyboard()
            updateList(filterStores())
        }
    }

    private fun filterStores(): List<Store> {
        val apiCategory = if (currentCategory == "전체") null else categoryToApi(currentCategory)
        val query = binding.etWishlistSearch.text?.toString()?.trim().orEmpty()

        return allStores.filter { store ->
            val matchesCategory = apiCategory == null || categoryToApi(store.category) == apiCategory
            val matchesQuery = query.isBlank() ||
                    store.name.contains(query, ignoreCase = true) ||
                    store.category.contains(query, ignoreCase = true) ||
                    store.address.contains(query, ignoreCase = true) ||
                    store.menus.any { it.name.contains(query, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etWishlistSearch.windowToken, 0)
        binding.etWishlistSearch.clearFocus()
    }

    private fun updateList(stores: List<Store>) {
        if (stores.isEmpty()) {
            binding.rvWishlist.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvWishlist.visibility = View.VISIBLE
            adapter.submitList(stores)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
