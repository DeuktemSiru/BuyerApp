package com.example.deuktemsiru_buyer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.storeCategoryFilters
import com.example.deuktemsiru_buyer.databinding.FragmentHomeBinding
import com.example.deuktemsiru_buyer.util.bindCategorySelection
import com.example.deuktemsiru_buyer.util.bindSearch
import com.example.deuktemsiru_buyer.util.updateCartBadge
import com.example.deuktemsiru_buyer.util.updateChipSelection
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var storeAdapter: StoreAdapter
    private lateinit var session: SessionManager
    private val categoryChips by lazy {
        listOf(
            binding.chipAll,
            binding.chipKorean,
            binding.chipWestern,
            binding.chipCafeDessert,
            binding.chipBakery,
            binding.chipCafe,
        ).zip(storeCategoryFilters).toMap()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        setupRecyclerView()
        setupSearch()
        setupCategoryChips()
        observeUiState()

        binding.btnCart.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_cart)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    storeAdapter.submitList(state.filteredStores)
                    binding.rvStores.isVisible = state.filteredStores.isNotEmpty() && !state.isLoading
                    binding.llEmpty.isVisible = state.filteredStores.isEmpty() && !state.isLoading && state.error == null

                    // Auth error → redirect to onboarding
                    if (state.authError) {
                        session.clear()
                        viewModel.authErrorHandled()
                        findNavController().navigate(
                            R.id.onboardingFragment,
                            null,
                            NavOptions.Builder().setPopUpTo(R.id.homeFragment, true).build()
                        )
                        return@collect
                    }

                    // General error → Snackbar
                    state.error?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                        viewModel.errorShown()
                    }

                    // Category chip visual sync
                    categoryChips.updateChipSelection(state.selectedCategory, requireContext())
                }
            }
        }
    }

    private fun setupRecyclerView() {
        storeAdapter = StoreAdapter(
            onStoreClick = { store ->
                findNavController().navigate(
                    R.id.action_home_to_storeDetail,
                    Bundle().apply { putLong("storeId", store.id) }
                )
            },
            onWishlistClick = { store ->
                if (!session.isLoggedIn()) return@StoreAdapter
                viewModel.toggleWishlist(store)
            }
        )
        binding.rvStores.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = storeAdapter
        }
    }

    private fun setupCategoryChips() {
        categoryChips.bindCategorySelection(
            fragment = this,
            selected = { viewModel.uiState.value.selectedCategory },
            onSelected = viewModel::selectCategory,
        )
    }

    private fun setupSearch() {
        bindSearch(binding.etSearch, binding.btnSearch) {
            viewModel.updateSearch(binding.etSearch.text?.toString().orEmpty())
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadStores(showLoading = false)
        _binding?.tvCartBadge?.updateCartBadge()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
