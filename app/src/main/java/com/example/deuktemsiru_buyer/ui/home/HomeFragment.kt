package com.example.deuktemsiru_buyer.ui.home

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.StoreRepository
import com.example.deuktemsiru_buyer.databinding.FragmentHomeBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.updateChipSelection
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(StoreRepository(RetrofitClient.api))
    }

    private lateinit var storeAdapter: StoreAdapter
    private lateinit var session: SessionManager
    private val categoryChips: Map<android.widget.TextView, String> by lazy {
        mapOf(
            binding.chipAll to "전체",
            binding.chipKorean to "한식",
            binding.chipWestern to "양식",
            binding.chipCafeDessert to "카페·디저트",
            binding.chipBakery to "베이커리",
            binding.chipCafe to "카페",
        )
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
                    syncCategoryChips(state.selectedCategory)
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
        categoryChips.forEach { (chip, category) ->
            chip.setOnClickListener { viewModel.selectCategory(category) }
        }
    }

    private fun syncCategoryChips(selected: String) {
        categoryChips.updateChipSelection(selected, requireContext())
    }

    private fun setupSearch() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.updateSearch(text?.toString() ?: "")
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
        binding.btnSearch.setOnClickListener { hideKeyboard() }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }

    private fun updateCartBadge() {
        if (_binding == null) return
        val count = CartManager.totalCount
        binding.tvCartBadge.isVisible = count > 0
        if (count > 0) binding.tvCartBadge.text = if (count > 9) "9+" else count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
