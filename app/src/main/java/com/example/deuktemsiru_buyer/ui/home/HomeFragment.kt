package com.example.deuktemsiru_buyer.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.categoryToApi
import com.example.deuktemsiru_buyer.data.toStore
import com.example.deuktemsiru_buyer.databinding.FragmentHomeBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var storeAdapter: StoreAdapter
    private var currentCategory = "전체"
    private val allStores = mutableListOf<Store>()
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        RetrofitClient.authToken = session.token
        setupRecyclerView()
        setupCategoryChips()
        loadStores(null)
    }

    private fun loadStores(category: String?) {
        lifecycleScope.launch {
            try {
                val apiCategory = if (category != null) categoryToApi(category) else null
                val userId = if (session.isLoggedIn()) session.userId else null
                val stores = RetrofitClient.api.getStores(apiCategory, userId).map { it.toStore() }
                allStores.clear()
                allStores.addAll(stores)
                updateList(stores)
            } catch (e: Exception) {
                if (e is HttpException && (e.code() == 401 || e.code() == 403)) {
                    session.clear()
                    RetrofitClient.authToken = null
                    Toast.makeText(requireContext(), "다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(
                        R.id.onboardingFragment,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .build()
                    )
                    return@launch
                }
                Toast.makeText(requireContext(), "가게 목록을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateList(stores: List<Store>) {
        if (stores.isEmpty()) {
            binding.rvStores.visibility = View.GONE
            binding.llEmpty.visibility = View.VISIBLE
        } else {
            binding.rvStores.visibility = View.VISIBLE
            binding.llEmpty.visibility = View.GONE
            storeAdapter.updateStores(stores)
        }
    }

    private fun setupRecyclerView() {
        storeAdapter = StoreAdapter(
            stores = allStores,
            onStoreClick = { store ->
                findNavController().navigate(
                    R.id.action_home_to_storeDetail,
                    bundleOf("storeId" to store.id)
                )
            },
            onWishlistClick = { store ->
                if (!session.isLoggedIn()) return@StoreAdapter
                lifecycleScope.launch {
                    try {
                        val result = RetrofitClient.api.toggleWishlist(store.id.toLong(), session.userId)
                        val isWishlisted = result["isWishlisted"] as? Boolean ?: false
                        val idx = allStores.indexOfFirst { it.id == store.id }
                        if (idx >= 0) {
                            allStores[idx] = allStores[idx].copy(isWishlisted = isWishlisted)
                            storeAdapter.updateStores(allStores.toList())
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "찜 처리 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvStores.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = storeAdapter
        }
    }

    private fun setupCategoryChips() {
        val chips = mapOf(
            binding.chipAll to "전체",
            binding.chipBakery to "베이커리",
            binding.chipLunchbox to "도시락",
            binding.chipSalad to "샐러드",
            binding.chipCafe to "카페"
        )

        chips.forEach { (chip, category) ->
            chip.setOnClickListener {
                currentCategory = category
                chips.forEach { (c, _) ->
                    c.setBackgroundResource(R.drawable.bg_chip_unselected)
                    c.setTextColor(Color.parseColor("#1A1A1A"))
                }
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(Color.WHITE)
                val apiCategory = if (category == "전체") null else category
                loadStores(apiCategory)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
