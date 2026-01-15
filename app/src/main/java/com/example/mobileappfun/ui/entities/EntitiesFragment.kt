package com.example.mobileappfun.ui.entities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mobileappfun.databinding.FragmentEntitiesBinding

class EntitiesFragment : Fragment() {

    private var _binding: FragmentEntitiesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // Set your adapter here
            // adapter = EntitiesAdapter(entities)
        }
    }

    private fun setupFab() {
        binding.fabAddEntity.setOnClickListener {
            // Handle adding new entity
            // Example: show dialog or navigate to add screen
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
