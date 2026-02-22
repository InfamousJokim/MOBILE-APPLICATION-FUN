# AI Prompts Used

## Game Flow
**Prompt:** Make the main menu accessible every time the game finishes. When pressing Play Game it should automatically start the game — no button needed on the camera page.

**Changes:**
- `HomeFragment.kt` — passes `autoStart = true` flag when navigating to camera
- `CameraFragment.kt` — reads flag, pre-sets front camera, auto-starts game after camera binds
- `CameraFragment.kt` — Close button on game result now navigates back to main menu

---

## Audio Setup
**Prompt:** Create a folder for game resources like music. Link the audio manager to the provided files:
- Background music looping during gameplay
- Correct gesture sound on successful gesture
- Failed sound on timeout or shake-to-skip
- Game completed sound at end of game

**Changes:**
- Created `app/src/main/res/raw/`
- Renamed files to valid Android resource names (lowercase, underscores)
- Rewrote `GameSoundManager.kt` — `MediaPlayer` for looping BGM, `SoundPool` for sound effects

---

## Bug Fix
**Prompt:** `Unresolved reference: findNavController` error in `CameraFragment`.

**Fix:** Added missing import `androidx.navigation.fragment.findNavController` to `CameraFragment.kt`

---

## Image Generation — Monkey Game Assets

**Tool:** Gemini (Nano Banana image-to-image model)
**Reference image:** `MONKEY GOOD.jpg` (Barbary macaque with harbour background)

### Prompts Used

| # | Prompt | Goal |
|---|---|---|
| 01 | "Generate a photo of the monkey holding up a fist for a game." | Neutral/ready fist pose |
| 02 | "I want the fist to be point wrist forward towards the viewer." | Adjust fist perspective toward camera |
| 03 | "Generate a photo of the monkey thumbs pointing down 'bad'." | Thumbs down / dislike gesture |
| 04 | "Generate a photo of a monkey with thumb point down 'bad'." | Refine thumbs down for anatomical accuracy |

### Observations
- The model maintained the monkey's visual identity (Barbary macaque + harbour background) across all iterations
- Non-human primate gestures like thumbs down were anatomically challenging — required multiple refinement passes
- Final assets serve as binary game feedback: `monkey_good` (success) and `monkey_thumbs_down` (fail)

### Output Files
- `monkey_fist.png` — closed fist facing viewer
- `monkey_thumbs_down.png` — thumbs down gesture
