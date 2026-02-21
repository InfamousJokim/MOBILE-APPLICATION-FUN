package com.example.mobileappfun.ui.camera.game

import androidx.annotation.DrawableRes
import com.example.mobileappfun.R

/**
 * Maps each recognizable gesture to its display name, emoji, instruction text,
 * and the corresponding monkey image resource.
 */
enum class GesturePrompt(
    val categoryName: String,
    val displayName: String,
    val emoji: String,
    val instruction: String,
    @DrawableRes val monkeyImageRes: Int
) {
    THUMBS_UP("Thumb_Up", "Thumbs Up", "\uD83D\uDC4D", "Give a thumbs up!", R.drawable.monkey_good),
    THUMBS_DOWN("Thumb_Down", "Thumbs Down", "\uD83D\uDC4E", "Thumbs down!", R.drawable.monkey_good),
    OPEN_PALM("Open_Palm", "Open Palm", "\u270B", "Show your open palm!", R.drawable.monkey_mouth),
    CLOSED_FIST("Closed_Fist", "Fist", "\u270A", "Make a fist!", R.drawable.monkey_mouth),
    PEACE_SIGN("Victory", "Peace Sign", "\u270C\uFE0F", "Show the peace sign!", R.drawable.monkey_up),
    POINTING_UP("Pointing_Up", "Point Up", "\u261D\uFE0F", "Point your finger up!", R.drawable.monkey_up),
    I_LOVE_YOU("ILoveYou", "I Love You", "\uD83E\uDD1F", "Show the I Love You sign!", R.drawable.monkey_heart);

    companion object {
        private val byCategory = entries.associateBy { it.categoryName }

        fun fromCategoryName(name: String): GesturePrompt? = byCategory[name]

        fun randomExcluding(exclude: GesturePrompt?): GesturePrompt {
            val candidates = if (exclude != null) entries.filter { it != exclude } else entries.toList()
            return candidates.random()
        }
    }
}
