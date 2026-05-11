package com.example.deuktemsiru_buyer.ui.wishlist

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.example.deuktemsiru_buyer.databinding.FragmentWishlistBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.ui.home.StoreAdapter
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

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        setupRecyclerView()
        setupCategoryChips()

        val session = SessionManager(requireContext())
        if (!session.isLoggedIn()) {
            binding.progress.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                val stores = RetrofitClient.api.getWishlist(session.userId).map { it.toStore() }
                allStores.clear()
                allStores.addAll(stores)
                binding.progress.visibility = View.GONE
                updateList(allStores)
            } catch (e: Exception) {
                binding.progress.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "찜 목록을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StoreAdapter(
            stores = emptyList(),
            onStoreClick = { store ->
                findNavController().navigate(
                    R.id.action_wishlist_to_storeDetail,
                    bundleOf("storeId" to store.id)
                )
            },
            onWishlistClick = {}
        )
        binding.rvWishlist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@WishlistFragment.adapter
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

                val filtered = if (category == "전체") allStores
                               else allStores.filter { it.category == category }
                updateList(filtered)
            }
        }
    }

    private fun updateList(stores: List<Store>) {
        if (stores.isEmpty()) {
            binding.rvWishlist.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvWishlist.visibility = View.VISIBLE
            adapter.updateStores(stores)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}