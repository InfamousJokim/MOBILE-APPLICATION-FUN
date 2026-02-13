package com.example.mobileappfun.ui.camera.game

/**
 * Maps each recognizable gesture to its display name, emoji, and instruction text.
 */
enum class GesturePrompt(
    val categoryName: String,
    val displayName: String,
    val emoji: String,
    val instruction: String
) {
    THUMBS_UP("Thumb_Up", "Thumbs Up", "\uD83D\uDC4D", "Give a thumbs up!"),
    THUMBS_DOWN("Thumb_Down", "Thumbs Down", "\uD83D\uDC4E", "Thumbs down!"),
    OPEN_PALM("Open_Palm", "Open Palm", "\u270B", "Show your open palm!"),
    CLOSED_FIST("Closed_Fist", "Fist", "\u270A", "Make a fist!"),
    PEACE_SIGN("Victory", "Peace Sign", "\u270C\uFE0F", "Show the peace sign!"),
    POINTING_UP("Pointing_Up", "Point Up", "\u261D\uFE0F", "Point your finger up!"),
    I_LOVE_YOU("ILoveYou", "I Love You", "\uD83E\uDD1F", "Show the I Love You sign!");

    companion object {
        private val byCategory = entries.associateBy { it.categoryName }

        fun fromCategoryName(name: String): GesturePrompt? = byCategory[name]

        fun randomExcluding(exclude: GesturePrompt?): GesturePrompt {
            val candidates = if (exclude != null) entries.filter { it != exclude } else entries.toList()
            return candidates.random()
        }
    }
}
