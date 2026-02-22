package com.example.mobileappfun.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mobileappfun.MainViewModel
import com.example.mobileappfun.MainViewModelFactory
import com.example.mobileappfun.R
import com.example.mobileappfun.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(requireActivity().applicationContext)
    }

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
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.playButton.setOnClickListener {
            findNavController().navigate(R.id.cameraFragment)
        }

        binding.leaderboardButton.setOnClickListener {
            findNavController().navigate(R.id.leaderboardFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.username.collect { username ->
                        binding.welcomeText.text = getString(R.string.welcome_back, username)
                    }
                }
                launch {
                    mainViewModel.personalBest.collect { best ->
                        if (best > 0) {
                            binding.bestScoreText.text = getString(R.string.best_score_value, best)
                        } else {
                            binding.bestScoreText.text = getString(R.string.no_score_yet)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshPersonalBest()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
