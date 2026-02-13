# TikTok-Style Gesture Matching Filter Game

A real-time gesture matching game that runs as an overlay on the camera screen. Random gestures appear on screen, and the player must perform them before a timer runs out. Correct matches trigger confetti and emoji effects on the detected face.

---

## How to Play

1. Open the **Camera** tab
2. Tap the **Play Game** button (bottom-left)
3. The camera auto-switches to front-facing and enables face + gesture detection
4. A **3-2-1-GO!** countdown starts the game
5. Each round shows a gesture prompt (emoji + instruction text)
6. Perform the gesture and **hold it for 0.6 seconds** to register a match
7. Match before the 5-second timer expires to earn points
8. After 10 rounds, a results screen shows your final score

---

## Game Flow

```
IDLE --> [Tap "Play Game"] --> COUNTDOWN (3..2..1..GO!)
  --> PLAYING_ROUND (5s timer, prompt shown, gesture checking)
    --> MATCHED (hold 600ms) --> confetti + crown + points --> 1.2s pause --> next round
    --> TIMEOUT (timer expires) --> sad face + streak reset --> 1s pause --> next round
  --> After 10 rounds --> GAME_OVER --> Result dialog (Play Again / Close)
```

---

## Scoring

| Component | Points | Details |
|-----------|--------|---------|
| Base | 10 | Awarded for each correct match |
| Time Bonus | 0-10 | Faster match = more bonus points |
| Streak Bonus | +5 | Awarded when streak reaches 3+ consecutive matches |
| **Max Theoretical** | **~250** | Perfect game (all 10 matched quickly with full streaks) |

---

## Supported Gestures

| Gesture | Emoji | Instruction |
|---------|-------|-------------|
| Thumbs Up | :thumbsup: | "Give a thumbs up!" |
| Thumbs Down | :thumbsdown: | "Thumbs down!" |
| Open Palm | :raised_hand: | "Show your open palm!" |
| Closed Fist | :fist: | "Make a fist!" |
| Peace Sign | :v: | "Show the peace sign!" |
| Point Up | :point_up: | "Point your finger up!" |
| I Love You | :love_you_gesture: | "Show the I Love You sign!" |

Consecutive rounds never repeat the same gesture.

---

## Visual Effects

### During Play
- **Timer Arc** at the top center counts down with color transitions (green > yellow > red)
- **Score** displayed top-left, animates when points are added
- **Round Counter** (e.g., "3/10") displayed top-right
- **Streak Indicator** shows fire emoji + count when streak >= 2
- **Ghost Emoji** floats semi-transparently above the detected face as a hint
- **Prompt Area** at bottom shows the target gesture emoji + instruction text

### On Correct Match
- **Confetti Burst** - 60 particles with physics (gravity, rotation, fade) in 8 colors
- **Crown Emoji** appears above the face and scales in
- **Green Glow Ring** pulses around the face
- **"+N points"** text floats upward and fades
- **"Matched!"** feedback text in green

### On Timeout
- **Sad Face Emoji** appears above the face
- **Red Glow Ring** pulses around the face
- **"Time's Up!"** feedback text in red
- Streak resets to 0

### Game Over
- Bottom sheet dialog with trophy emoji
- Final score in large gold text
- Accuracy stat (e.g., "7/10") with target emoji
- Best streak stat with fire emoji
- Title changes based on performance:
  - 10/10: "Star Perfect! Star"
  - 8+/10: "Great Job!"
  - 5+/10: "Game Over"
  - Below 5: "Keep Practicing!"
- **Play Again** and **Close** buttons

---

## Architecture

### New Files

```
ui/camera/game/
  GesturePrompt.kt      -- Enum mapping gestures to display data
  GestureGame.kt         -- State machine, scoring, timer, hold detection
  GameOverlayView.kt     -- Canvas HUD (countdown, timer, score, prompts)
  GameFaceEffectsView.kt -- Emoji overlays positioned on detected face
  ParticleEffectView.kt  -- Confetti particle system
  GameResultDialog.kt    -- End-of-game BottomSheetDialogFragment

res/layout/
  dialog_game_result.xml -- Result dialog layout

res/drawable/
  ic_game.xml            -- Gamepad vector icon
```

### Modified Files

```
ui/camera/CameraFragment.kt  -- Game lifecycle, callback wiring, data forwarding
res/layout/fragment_camera.xml -- 3 overlay views + Play Game button
res/values/strings.xml         -- 12 game string resources
res/values/colors.xml          -- 5 game colors
```

### View Stacking Order (bottom to top)

1. `PreviewView` - Camera feed
2. `FaceOverlayView` - Existing face landmarks (stay visible during game)
3. `HandOverlayView` - Existing hand skeleton (stays visible during game)
4. `GameFaceEffectsView` - Emoji effects on face
5. `ParticleEffectView` - Confetti particles
6. `GameOverlayView` - HUD on top of everything

### Key Design Decisions

- **No new dependencies** - All effects use Canvas drawing only
- **600ms hold requirement** prevents flicker false positives from gesture detection
- **Auto camera switch** - Game forces front camera and enables both face + gesture detection
- **State preservation** - Pre-game detection toggles and camera selection are saved and restored when the game ends
- **Coordinate mirroring** - `GameFaceEffectsView` uses the same coordinate mapping as `FaceOverlayView` (`scaleX = viewWidth / imageHeight`, `scaleY = viewHeight / imageWidth`) with front-camera horizontal flip

---

## Dependencies

No new dependencies were added. The game uses the existing:

- **ML Kit Face Detection** - For face position tracking (emoji placement)
- **MediaPipe Gesture Recognizer** - For hand gesture recognition (game input)
- **CameraX** - Camera preview and image analysis
- **Material Design 3** - Play Game button and result dialog styling
