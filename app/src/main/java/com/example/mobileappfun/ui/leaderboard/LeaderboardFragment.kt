package com.example.mobileappfun.ui.leaderboard

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileappfun.data.UserRepository
import com.example.mobileappfun.data.db.AppDatabase
import com.example.mobileappfun.data.db.LeaderboardEntry
import com.example.mobileappfun.databinding.FragmentLeaderboardBinding
import com.example.mobileappfun.databinding.ItemLeaderboardBinding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────

class LeaderboardViewModel(repository: UserRepository) : ViewModel() {

    val leaderboard: StateFlow<List<LeaderboardEntry>> =
        repository.getLeaderboard()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class LeaderboardViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(context)
        val repo = UserRepository(db)
        return LeaderboardViewModel(repo) as T
    }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class LeaderboardAdapter : ListAdapter<LeaderboardEntry, LeaderboardAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LeaderboardEntry>() {
            override fun areItemsTheSame(a: LeaderboardEntry, b: LeaderboardEntry) =
                a.username == b.username && a.score == b.score
            override fun areContentsTheSame(a: LeaderboardEntry, b: LeaderboardEntry) = a == b
        }

        private val RANK_COLORS = mapOf(
            1 to "#FFD700",   // Gold
            2 to "#C0C0C0",   // Silver
            3 to "#CD7F32"    // Bronze
        )

        private val RANK_EMOJIS = mapOf(
            1 to "🥇",
            2 to "🥈",
            3 to "🥉"
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(private val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LeaderboardEntry, rank: Int) {
            val emoji = RANK_EMOJIS[rank]
            if (emoji != null) {
                binding.rankText.text = emoji
                binding.rankText.textSize = 20f
            } else {
                binding.rankText.text = "#$rank"
                binding.rankText.textSize = 14f
            }

            val rankColor = RANK_COLORS[rank] ?: "#6060A0"
            binding.rankText.setTextColor(Color.parseColor(rankColor))

            binding.usernameText.text = entry.username
            binding.scoreText.text = entry.score.toString()
            binding.accuracyText.text =
                binding.root.context.getString(
                    com.example.mobileappfun.R.string.leaderboard_accuracy,
                    entry.wallsCleared
                )

            // Highlight top 3 rows slightly
            binding.root.setBackgroundColor(
                when (rank) {
                    1 -> Color.parseColor("#1F1A0F")
                    2 -> Color.parseColor("#141414")
                    3 -> Color.parseColor("#1A100A")
                    else -> Color.parseColor("#12122A")
                }
            )
        }
    }
}

// ── Fragment ─────────────────────────────────────────────────────────────────

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LeaderboardViewModel
    private val adapter = LeaderboardAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            LeaderboardViewModelFactory(requireActivity().applicationContext)
        )[LeaderboardViewModel::class.java]

        binding.leaderboardRecyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.leaderboard.collect { entries ->
                    adapter.submitList(entries)
                    binding.emptyState.visibility =
                        if (entries.isEmpty()) View.VISIBLE else View.GONE
                    binding.leaderboardRecyclerView.visibility =
                        if (entries.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
