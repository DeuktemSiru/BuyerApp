package com.example.deuktemsiru_buyer.ui.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.StoreRepository
import com.example.deuktemsiru_buyer.data.storeCategoryFilters
import com.example.deuktemsiru_buyer.databinding.FragmentWishlistBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.ui.home.StoreAdapter
import com.example.deuktemsiru_buyer.util.Result
import com.example.deuktemsiru_buyer.util.bindCategorySelection
import com.example.deuktemsiru_buyer.util.bindSearch
import com.example.deuktemsiru_buyer.util.filterStores
import kotlinx.coroutines.launch

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    private val allStores = mutableListOf<Store>()
    private var currentCategory = "전체"
    private lateinit var adapter: StoreAdapter
    private val repository by lazy { StoreRepository(RetrofitClient.api) }

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
            when (val result = repository.getWishlist()) {
                is Result.Success -> {
                    val stores = result.data
                    allStores.clear()
                    allStores.addAll(stores)
                    binding.progress.visibility = View.GONE
                    updateList(filterStores())
                }
                is Result.Error -> {
                    binding.progress.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "찜 목록을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StoreAdapter(
            onStoreClick = { store ->
                findNavController().navigate(
                    R.id.action_wishlist_to_storeDetail,
                    Bundle().apply { putLong("storeId", store.id) }
                )
            },
            onWishlistClick = { store ->
                viewLifecycleOwner.lifecycleScope.launch {
                    when (repository.toggleWishlist(store.id)) {
                        is Result.Success -> {
                        allStores.removeAll { it.id == store.id }
                        updateList(filterStores())
                        Toast.makeText(requireContext(), "찜 목록에서 제거했어요", Toast.LENGTH_SHORT).show()
                        }
                        is Result.Error -> {
                        Toast.makeText(requireContext(), "찜 처리 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                        }
                        is Result.Loading -> Unit
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
        val chips = listOf(
            binding.chipAll,
            binding.chipKorean,
            binding.chipWestern,
            binding.chipCafeDessert,
            binding.chipBakery,
            binding.chipCafe,
        ).zip(storeCategoryFilters).toMap()

        chips.bindCategorySelection(
            fragment = this,
            selected = { currentCategory },
            onSelected = {
                currentCategory = it
                updateList(filterStores())
            },
        )
    }

    private fun setupSearch() {
        bindSearch(binding.etWishlistSearch, binding.btnWishlistSearch) { updateList(filterStores()) }
    }

    private fun filterStores(): List<Store> {
        val query = binding.etWishlistSearch.text?.toString()?.trim().orEmpty()
        return allStores.filterStores(currentCategory, query)
    }

    private fun updateList(stores: List<Store>) {
        adapter.submitList(stores)
        binding.rvWishlist.visibility = if (stores.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (stores.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
