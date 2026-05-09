package com.example.deuktemsiru_buyer.ui.wishlist

import android.os.Bundle
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
import com.example.deuktemsiru_buyer.data.toStore
import com.example.deuktemsiru_buyer.databinding.FragmentWishlistBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.ui.home.StoreAdapter
import kotlinx.coroutines.launch

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

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

        val session = SessionManager(requireContext())
        if (!session.isLoggedIn()) return

        lifecycleScope.launch {
            try {
                val stores = RetrofitClient.api.getWishlist().data
                    ?.map { it.toStore() }
                    ?: emptyList()
                val adapter = StoreAdapter(
                    stores = stores,
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
                    this.adapter = adapter
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "찜 목록을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
