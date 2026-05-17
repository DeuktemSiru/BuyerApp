package com.example.deuktemsiru_buyer.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentSiruLinkBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.network.SiruLinkRequest
import kotlinx.coroutines.launch

class SiruLinkFragment : Fragment() {

    private var _binding: FragmentSiruLinkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSiruLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = SessionManager(requireContext())

        binding.btnLinkSiru.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching {
                    RetrofitClient.api.linkSiru(SiruLinkRequest("local-siru-${session.memberId}")).data
                }.onSuccess { member ->
                    session.isSiruLinked = member?.isSiruLinked ?: true
                    session.siruBalance = member?.siruBalance ?: 0
                    Toast.makeText(requireContext(), "시루 계정이 연동됐어요.", Toast.LENGTH_SHORT).show()
                    navigateAfterLink()
                }.onFailure {
                    Toast.makeText(requireContext(), "시루 연동에 실패했어요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnSkip.setOnClickListener {
            if (arguments?.getBoolean("returnToPayment") == true) {
                findNavController().popBackStack()
            } else {
                findNavController().navigate(R.id.action_siru_to_home)
            }
        }
    }

    private fun navigateAfterLink() {
        if (arguments?.getBoolean("returnToPayment") == true) {
            findNavController().navigate(
                R.id.paymentFragment,
                Bundle().apply {
                    putLong("storeId", arguments?.getLong("storeId") ?: 0L)
                    putLong("menuId", arguments?.getLong("menuId") ?: 0L)
                    putInt("totalPrice", arguments?.getInt("totalPrice") ?: 0)
                    putBoolean("fromCart", arguments?.getBoolean("fromCart") ?: false)
                    putBoolean("autoPayAfterLink", true)
                },
                NavOptions.Builder()
                    .setPopUpTo(R.id.siruLinkFragment, true)
                    .build(),
            )
        } else {
            findNavController().navigate(R.id.action_siru_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
