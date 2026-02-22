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
    THUMBS_UP("Thumb_Up",       "Thumbs Up",       "\uD83D\uDC4D", "Give a thumbs up!",                R.drawable.monkey_good),
    THUMBS_DOWN("Thumb_Down",   "Thumbs Down",     "\uD83D\uDC4E", "Give a thumbs down!",              R.drawable.monkey_thumbs_down),
    OPEN_PALM("Open_Palm",      "Open Palm",       "\u270B",       "Show your open palm!",             R.drawable.monkey_up),
    CLOSED_FIST("Closed_Fist",  "Fist",            "\u270A",       "Make a fist!",                     R.drawable.monkey_fist),
    PEACE_SIGN("Victory",       "Peace Sign",      "\u270C\uFE0F", "Show the peace sign!",             R.drawable.monkey_peace),
    FINGER_ON_MOUTH("Finger_On_Mouth", "Shhh",     "\uD83E\uDD2B", "Put your finger on your mouth!",  R.drawable.monkey_mouth),
    I_LOVE_YOU("ILoveYou",      "I Love You",      "\uD83E\uDD1F", "Show the I Love You sign!",        R.drawable.monkey_heart),
    KOREAN_HEART("Korean_Heart","Korean Heart",    "\uD83E\uDEB7", "Make a Korean finger heart!",      R.drawable.monkey_heart);

    companion object {
        private val byCategory = entries.associateBy { it.categoryName }

        fun fromCategoryName(name: String): GesturePrompt? = byCategory[name]

        fun randomExcluding(exclude: GesturePrompt?): GesturePrompt {
            val candidates = if (exclude != null) entries.filter { it != exclude } else entries.toList()
            return candidates.random()
        }
    }
}
