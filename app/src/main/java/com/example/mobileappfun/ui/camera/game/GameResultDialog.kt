package com.example.mobileappfun.ui.camera.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mobileappfun.R
import com.example.mobileappfun.databinding.DialogGameResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet dialog showing final game results:
 * score, accuracy, best streak, and play-again button.
 */
class GameResultDialog : BottomSheetDialogFragment() {

    private var _binding: DialogGameResultBinding? = null
    private val binding get() = _binding!!

    var onPlayAgain: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null

    companion object {
        const val TAG = "GameResultDialog"
        private const val ARG_SCORE = "score"
        private const val ARG_CORRECT = "correct"
        private const val ARG_TOTAL = "total"
        private const val ARG_BEST_STREAK = "best_streak"

        fun newInstance(score: Int, correct: Int, total: Int, bestStreak: Int): GameResultDialog {
            return GameResultDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SCORE, score)
                    putInt(ARG_CORRECT, correct)
                    putInt(ARG_TOTAL, total)
                    putInt(ARG_BEST_STREAK, bestStreak)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogGameResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val score = arguments?.getInt(ARG_SCORE, 0) ?: 0
        val correct = arguments?.getInt(ARG_CORRECT, 0) ?: 0
        val total = arguments?.getInt(ARG_TOTAL, 0) ?: 0
        val bestStreak = arguments?.getInt(ARG_BEST_STREAK, 0) ?: 0

        binding.finalScoreText.text = score.toString()
        binding.accuracyText.text = getString(R.string.game_accuracy, correct, total)
        binding.bestStreakText.text = bestStreak.toString()

        // Update title based on performance
        val titleText = when {
            correct == total -> "\uD83C\uDF1F Perfect! \uD83C\uDF1F"
            correct >= total * 0.8 -> getString(R.string.game_great_job)
            correct >= total * 0.5 -> getString(R.string.game_over)
            else -> "Keep Practicing!"
        }
        binding.gameOverTitle.text = titleText

        binding.playAgainButton.setOnClickListener {
            dismiss()
            onPlayAgain?.invoke()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
            onClose?.invoke()
        }

        // Prevent dismissing by tapping outside during result
        isCancelable = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
