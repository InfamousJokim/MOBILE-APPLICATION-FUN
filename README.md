# Brain Rot Knowledge — Gesture Recognition Game

An Android application built in Kotlin that combines real-time computer vision with an interactive gesture-matching game. Players use hand gestures and facial expressions captured through the device camera to compete for high scores on a local leaderboard.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Authentication System](#authentication-system)
4. [Navigation & Screen Structure](#navigation--screen-structure)
5. [Camera System](#camera-system)
6. [Face Detection](#face-detection)
7. [Hand Gesture Recognition](#hand-gesture-recognition)
8. [The Gesture Game](#the-gesture-game)
   - [Game Flow & State Machine](#game-flow--state-machine)
   - [Gesture Prompts](#gesture-prompts)
   - [Scoring System](#scoring-system)
   - [Shake-to-Skip Mechanic](#shake-to-skip-mechanic)
9. [Visual Effects System](#visual-effects-system)
10. [Audio System](#audio-system)
11. [Data Layer — Room Database](#data-layer--room-database)
12. [Leaderboard](#leaderboard)
13. [Technology Stack & Dependencies](#technology-stack--dependencies)
14. [Permissions](#permissions)

---

## Project Overview

**Brain Rot Knowledge** is a mobile game for Android (API 24+) where players match hand gestures shown on screen in real time. The app uses two separate ML pipelines running simultaneously on every camera frame:

- **Google ML Kit Face Detection** — tracks facial landmarks and expressions
- **Google MediaPipe Gesture Recognizer** — classifies hand gestures from a 21-point landmark model

These two streams are fused together to support composite gestures (e.g., "Finger on Mouth") that require both hand position and face position data to detect correctly.

The game features a complete user account system with SHA-256 hashed passwords, a persistent Room database with two relational tables, a reactive leaderboard powered by Kotlin Flows, and a rich set of canvas-drawn visual effects and audio feedback.

---

## Architecture

The application follows the **MVVM (Model-View-ViewModel)** architectural pattern combined with the **Repository pattern** for data access, as recommended by Google's Android Architecture Guidelines.

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│  LoginScreen (Compose)  │  Fragments (View Binding)     │
│  LoginActivity          │  MainActivity                 │
└──────────────┬──────────┴────────────┬──────────────────┘
               │ observes StateFlow    │ observes StateFlow
┌──────────────▼──────────────────────▼──────────────────┐
│                     ViewModel Layer                     │
│  LoginViewModel   │  MainViewModel   │  LeaderboardVM   │
└──────────────┬────┴──────────────────┴────────────────┬─┘
               │ calls suspend fns / collects Flows      │
┌──────────────▼─────────────────────────────────────────▼┐
│                    Repository Layer                      │
│              UserRepository                             │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│                     Data Layer                          │
│  AppDatabase (Room)  │  UserDao  │  ScoreDao            │
│  UserEntity          │  ScoreEntity                     │
└─────────────────────────────────────────────────────────┘
```

### Key Design Patterns Used

| Pattern | Where Applied |
|---|---|
| MVVM | All screens — ViewModels expose `StateFlow` consumed by UI |
| Repository | `UserRepository` abstracts DAOs from ViewModels |
| Observer (Kotlin Flow) | Leaderboard and personal best update reactively without polling |
| Singleton (double-checked locking) | `AppDatabase.getInstance()` — thread-safe single instance |
| State Machine | `GestureGame` — explicit `State` enum drives all game logic |
| Callback Interface | `GestureGame.GameCallback` and `HandGestureHelper.GestureListener` decouple detection from UI |
| ViewBinding | All Fragment UIs — null-safe access to XML views |
| Jetpack Compose (UDF) | Login screen — unidirectional data flow, state hoisted to ViewModel |

---

## Authentication System

### LoginActivity & LoginScreen

The app entry point is `LoginActivity`, which hosts a **Jetpack Compose** login/registration screen (`LoginScreen`). This is one of two UI toolkits used in the project — the rest of the app uses traditional XML layouts with View Binding.

**Flow:**
1. App launches → `LoginActivity` shown (declared as the launcher in `AndroidManifest.xml`)
2. User enters credentials and taps **Log In** or **Create Account**
3. `LoginViewModel` calls the appropriate method on `UserRepository`
4. On success → `navigateToMain()` fires, passing the authenticated user's `id` and `username` as Intent Extras to `MainActivity`
5. `LoginActivity.finish()` is called so the back button cannot return to the login screen

### Password Security

Passwords are **never stored in plaintext**. The `UserRepository.sha256()` utility hashes the password using `MessageDigest` (SHA-256) and stores the 64-character lowercase hex digest in the `users` table. On login, the entered password is hashed and compared to the stored hash.

### Auth Result Sealed Class

The `AuthResult` sealed class models all possible outcomes of an auth operation:

```kotlin
sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    object UsernameTaken       : AuthResult()
    object InvalidCredentials  : AuthResult()
    data class Error(val message: String) : AuthResult()
}
```

The ViewModel maps these outcomes to UI states (`errorMessage`, `isLoading`, `authenticatedUser`) consumed by the Compose screen via `collectAsState()`.

---

## Navigation & Screen Structure

Navigation is handled by the **Jetpack Navigation Component** with a bottom navigation bar. `MainActivity` hosts the `NavHostFragment` and the `BottomNavigationView`, which is wired to the nav controller via `setupWithNavController()`.

### Screens

| Screen | Class | Type |
|---|---|---|
| Login / Register | `LoginActivity` + `LoginScreen` | Activity + Compose |
| Main Host | `MainActivity` | Activity |
| Home (Main Menu) | `HomeFragment` | Fragment |
| Camera & Game | `CameraFragment` | Fragment |
| Leaderboard | `LeaderboardFragment` | Fragment |
| Settings | `SettingsFragment` | Fragment |

### Home Screen (Main Menu)

`HomeFragment` displays a personalised welcome message using the logged-in username (observed from `MainViewModel.username` StateFlow) and the player's current personal best score (`MainViewModel.personalBest`). It has two primary actions:

- **Play** — navigates to `CameraFragment` with `autoStart = true`, which automatically switches to the front camera and starts the game immediately
- **Leaderboard** — navigates to `LeaderboardFragment`

---

## Camera System

The camera is managed in `CameraFragment` using **CameraX** from the Jetpack library suite. Three CameraX use cases are bound simultaneously to the fragment's lifecycle:

| Use Case | Purpose |
|---|---|
| `Preview` | Renders live camera feed to the `PreviewView` |
| `ImageCapture` | Captures full-resolution photos saved to `MediaStore` |
| `ImageAnalysis` | Delivers frames to the `CombinedAnalyzer` for ML processing |

### CombinedAnalyzer

The `CombinedAnalyzer` is an inner class implementing `ImageAnalysis.Analyzer`. On every frame it:

1. Runs **gesture detection** first (synchronous bitmap conversion from `ImageProxy`)
2. Then submits the same frame to **face detection** (asynchronous via ML Kit's task API)
3. Closes `ImageProxy` in the appropriate completion listener to prevent memory leaks

The `ImageAnalysis` backpressure strategy is set to `STRATEGY_KEEP_ONLY_LATEST`, ensuring only the most recent frame is processed and older frames are dropped — preventing queue buildup under load.

### Camera Switching

Front and back cameras can be toggled at any time. When switching, the `HandGestureHelper` is fully closed and re-initialised because MediaPipe's `LIVE_STREAM` mode requires strictly increasing timestamps, which reset when a new camera stream begins.

The app gracefully handles devices (such as emulators) with only one camera by checking `cameraProvider.hasCamera()` before binding and falling back to the available camera.

### Photo Capture

The capture button saves photos to the device's `MediaStore` under `Pictures/MobileAppFun` using `ImageCapture.OutputFileOptions`. The gallery button opens a `PhotoGalleryBottomSheet` that displays saved images in a `RecyclerView`.

---

## Face Detection

Face detection is powered by **Google ML Kit Face Detection** (`com.google.mlkit:face-detection:16.1.6`).

### Configuration

```kotlin
FaceDetectorOptions.Builder()
    .setPerformanceMode(PERFORMANCE_MODE_FAST)
    .setLandmarkMode(LANDMARK_MODE_ALL)
    .setClassificationMode(CLASSIFICATION_MODE_ALL)
    .setMinFaceSize(0.15f)
    .enableTracking()
    .build()
```

- **Fast mode** prioritises frame rate over landmark precision
- **Tracking enabled** assigns persistent face IDs across frames
- **Minimum face size 15%** prevents false positives from small faces in the background

### Detected Landmarks (10 points)

| Landmark | Use |
|---|---|
| Left Eye / Right Eye | Drawn as yellow dots on `FaceOverlayView` |
| Nose Base | Drawn on overlay |
| Mouth Left / Right / Bottom | Mouth Bottom used for **Finger on Mouth** composite gesture detection |
| Left/Right Ear | Drawn on overlay |
| Left/Right Cheek | Drawn on overlay |

### Facial Expressions

The `FaceOverlayView` classifies and labels the following states in real time:

| Expression | Threshold |
|---|---|
| Smiling | `smilingProbability > 0.5` |
| Eyes Closed | Both eyes open probability `< 0.3` |
| Looking Left | `headEulerAngleY > 20°` |
| Looking Right | `headEulerAngleY < -20°` |

### Coordinate Mapping

ML Kit returns face bounding boxes and landmarks in rotated-image space where `x ∈ [0..imageHeight]` and `y ∈ [0..imageWidth]`. Both `FaceOverlayView` and `GameFaceEffectsView` apply the same normalisation:

```kotlin
val scaleX = viewWidth / imageHeight
val scaleY = viewHeight / imageWidth
// Front camera: mirror X
left = viewWidth - bounds.right * scaleX
```

The mouth position is separately normalised to `[0..1]` and mirrored for the front camera before being passed to the gesture resolver for composite gesture detection.

---

## Hand Gesture Recognition

Hand gesture recognition is powered by **Google MediaPipe Tasks Vision** (`com.google.mediapipe:tasks-vision:0.10.9`), using a bundled `gesture_recognizer.task` model file stored in the `assets/` directory.

### HandGestureHelper

`HandGestureHelper` wraps the MediaPipe `GestureRecognizer` and exposes a `GestureListener` callback interface:

```kotlin
interface GestureListener {
    fun onGestureResult(result: GestureRecognizerResult?, imageWidth: Int, imageHeight: Int)
    fun onGestureError(error: String)
}
```

**Configuration:**
- Running mode: `LIVE_STREAM` (asynchronous, result delivered via callback)
- Delegate: `CPU`
- Max hands: `2`
- Detection/tracking/presence confidence thresholds: `0.5`

Each frame is converted to a `Bitmap`, rotated according to `imageProxy.imageInfo.rotationDegrees`, and horizontally mirrored for the front camera before being submitted via `recognizeAsync()`.

### Gesture Category Resolution

The static `HandGestureHelper.resolveCategory()` function applies a **priority-based resolution** to determine the effective gesture:

```
Priority 1: Korean Heart (custom landmark geometry)
Priority 2: Finger On Mouth (custom landmark + face position)
Priority 3: MediaPipe model's own classification
```

This allows custom gestures that the base model does not natively classify to be detected on top of the standard gesture set.

### Supported Gestures

| Display Name | Category Name | Detection Method |
|---|---|---|
| Thumbs Up | `Thumb_Up` | Model |
| Thumbs Down | `Thumb_Down` | Model |
| Open Palm | `Open_Palm` | Model |
| Closed Fist | `Closed_Fist` | Model |
| Peace Sign | `Victory` | Model |
| I Love You | `ILoveYou` | Model |
| Korean Heart | `Korean_Heart` | Custom landmark geometry |
| Shhh (Finger on Mouth) | `Finger_On_Mouth` | Custom landmark + face position |

### Custom Gesture: Korean Heart (손하트)

The Korean finger heart is not part of the base MediaPipe gesture vocabulary. It is detected using a pure landmark geometry algorithm in `isKoreanHeart()`:

1. Compute hand size as the wrist → middle MCP distance (scale reference)
2. Check that thumb tip (landmark 4) and index tip (landmark 8) are within **40% of hand size** of each other
3. Verify that middle (12), ring (16), and pinky (20) fingers are curled — their tips must be no further from the wrist than 1.3× the distance of their PIP joint from the wrist

### Custom Gesture: Finger on Mouth

Detected by `isFingerOnMouth()` using a combination of hand landmarks and the face's mouth position (supplied by ML Kit):

1. Index finger must be extended: tip further from wrist than MCP by a factor of ≥ 1.6
2. Index tip must be within **18% of normalised screen width** from the detected mouth landmark
3. Middle, ring, and pinky fingers must be curled

---

## The Gesture Game

### Game Flow & State Machine

The core game logic lives in `GestureGame.kt`, which is a pure business-logic class with no Android UI dependencies. It communicates entirely through the `GameCallback` interface implemented by `CameraFragment`.

```
IDLE
  │ startGame()
  ▼
COUNTDOWN (3 → 2 → 1 → GO!)
  │ onFinish + 500ms delay
  ▼
PLAYING_ROUND <──────────────────────────────────────────┐
  │                                                       │
  ├─ gesture held for 600ms ──► MATCHED                  │
  │    • calculate points (base + time + streak bonus)    │
  │    • 1,200ms pause                                    │
  │    • startNextRound() ────────────────────────────────┘
  │
  ├─ 5s timer expires ──────► TIMEOUT                    │
  │    • streak reset to 0                                │
  │    • 1,000ms pause                                    │
  │    • startNextRound() ────────────────────────────────┘
  │
  └─ after round 10 ────────► GAME_OVER
       • score saved to Room database
       • GameResultDialog (BottomSheet) shown
```

**State enum:**
```kotlin
enum class State { IDLE, COUNTDOWN, PLAYING_ROUND, MATCHED, TIMEOUT, GAME_OVER }
```

State transitions are broadcast to `CameraFragment` via `onStateChanged()`, allowing the UI to react to every change.

### Starting the Game

When the Play button is tapped (or the Home screen's Play button is used with `autoStart = true`), `CameraFragment.startGame()`:

1. Saves the current detection toggle states and camera selector
2. Force-enables both face detection and gesture detection
3. Switches to the **front-facing camera** (required so the player can see themselves while gesturing)
4. Initialises `GameSoundManager` and starts background music
5. Creates a new `GestureGame` instance and calls `startGame()`
6. Shows game overlay views and hides the normal camera UI controls

When the game ends or is closed, `stopGame()` reverses all of these changes, restoring the pre-game state exactly.

### Gesture Prompts

`GesturePrompt` is a Kotlin `enum class` that maps each playable gesture to all its display properties:

```kotlin
enum class GesturePrompt(
    val categoryName: String,   // matches MediaPipe/custom category name
    val displayName: String,    // shown in result dialog
    val emoji: String,          // shown as ghost hint above the player's face
    val instruction: String,    // shown in the HUD prompt area
    val monkeyImageRes: Int     // monkey image shown in the HUD circular prompt
)
```

The 8 playable gestures are:

| Gesture | Emoji | Instruction |
|---|---|---|
| Thumbs Up | 👍 | "Give a thumbs up!" |
| Thumbs Down | 👎 | "Give a thumbs down!" |
| Open Palm | ✋ | "Show your open palm!" |
| Closed Fist | ✊ | "Make a fist!" |
| Peace Sign | ✌️ | "Show the peace sign!" |
| Shhh | 🤫 | "Put your finger on your mouth!" |
| I Love You | 🤟 | "Show the I Love You sign!" |
| Korean Heart | 🫰 | "Make a Korean finger heart!" |

`GesturePrompt.randomExcluding(lastPrompt)` ensures consecutive rounds never show the same gesture.

### Scoring System

| Component | Points | Condition |
|---|---|---|
| Base points | 10 | Any correct match |
| Time bonus | 0 – 10 | Proportional to how quickly the gesture was matched within the 5-second window |
| Streak bonus | +5 | Applied when the current streak reaches 3 or more consecutive matches |

**Time bonus calculation:**
```kotlin
val elapsed = System.currentTimeMillis() - (holdStartTime - HOLD_DURATION_MS)
val fraction = 1.0 - (elapsed / ROUND_TIME_MS).coerceIn(0.0, 1.0)
val timeBonus = (fraction * MAX_TIME_BONUS).toInt()
```

A perfect 10-round game (all matched instantly, full streak from round 3) yields approximately **250 points**.

**Hold detection:** To prevent flickering false positives, a match only registers after the correct gesture has been continuously detected for **600 milliseconds**. If the gesture breaks mid-hold, the hold timer resets.

### Shake-to-Skip Mechanic

The accelerometer sensor (`Sensor.TYPE_ACCELEROMETER`) is registered at `SENSOR_DELAY_GAME` priority during active game sessions. The net acceleration above Earth's gravitational constant is computed each sensor event:

```kotlin
val netAcceleration = sqrt(x² + y² + z²).toFloat() - SensorManager.GRAVITY_EARTH
```

If `netAcceleration > 8 m/s²` and at least **1,500 ms** have elapsed since the last shake, `gestureGame.skipRound()` is triggered. This immediately ends the current round as a timeout (resetting the streak), plays the failure sound, and shows a "Skipped! 🙈" animation. The hint "📱 Shake to skip" is permanently rendered at the bottom of the HUD.

---

## Visual Effects System

All visual effects are implemented as custom Android `View` subclasses using the `Canvas` API — no third-party animation libraries are required.

### View Stacking Order

The `fragment_camera.xml` layout stacks six views using `FrameLayout`:

| Order (bottom → top) | View | Purpose |
|---|---|---|
| 1 | `PreviewView` | Live camera feed |
| 2 | `FaceOverlayView` | Face bounding boxes and landmarks |
| 3 | `HandOverlayView` | Hand skeleton (21 landmarks, 23 connections) |
| 4 | `GameFaceEffectsView` | Emoji effects anchored to the detected face position |
| 5 | `ParticleEffectView` | Confetti particle system |
| 6 | `GameOverlayView` | HUD — countdown, timer, score, prompts, feedback text |

### GameOverlayView (HUD)

Drawn entirely on `Canvas` with no XML layout. All text and graphics use pre-allocated `Paint` objects and animated via `ValueAnimator`. Elements rendered:

- **Countdown** — large animated text ("3", "2", "1", "GO!") with an `OvershootInterpolator` bounce-in scale animation
- **Score bar** — current score top-left and round counter (e.g. "3/10") top-right; score increments animate with `ValueAnimator.ofInt`
- **Timer arc** — circular arc swept from 360° down to 0° over 5 seconds; colour transitions green → yellow → red based on remaining fraction
- **Monkey image prompt** — the target gesture's monkey image displayed in a circular clip with a gold glow ring; pops in with `OvershootInterpolator` scale animation on each new round
- **Instruction text** — displayed below the monkey image (e.g. "Give a thumbs up!")
- **Streak indicator** — "🔥 x3" in gold, visible when streak ≥ 2
- **Match feedback** — "Matched!" text in green, floats upward and fades using `DecelerateInterpolator`
- **Points pop** — "+15" text in green, floats upward independently of the feedback text
- **Timeout feedback** — "Time's Up!" text in red with the same animation
- **Shake feedback** — "Skipped! 🙈" text in purple, triggered by a detected shake
- **Monkey border pulse** — the circular border around the monkey image pulses to green (match), red (timeout), or purple (skip), then interpolates back to white
- **Shake hint** — "📱 Shake to skip" permanently rendered at the bottom of the screen

### GameFaceEffectsView

Positioned on top of the camera feed, this view draws emoji effects at the position of the detected face. It uses the identical coordinate mapping as `FaceOverlayView` for accurate alignment:

- **Ghost emoji** — the target gesture's emoji rendered semi-transparently (alpha 100/255) above the player's head throughout each round as a visual hint
- **Crown emoji (👑)** — appears above the head on a correct match; scales in with `DecelerateInterpolator` and fades over 1 second
- **Sad face emoji (😞)** — appears on timeout with the same animation
- **Glow ring** — a coloured circle drawn around the face bounding box; pulses in/out with `ValueAnimator.ofFloat(0f, 1f, 0f)` over 800 ms (green = match, red = timeout)

### ParticleEffectView

A physics-based confetti burst system triggered on every correct gesture match:

- Spawns **60 particles** per burst from the upper-centre of the screen
- Each particle has randomised: angle, speed (300–800 px/s), rotation speed (±360°/s), size (8–20 px), colour (8-colour palette), and decay rate
- Physics applied per frame: `vy += GRAVITY * dt` (GRAVITY = 800 px/s²), position updated by velocity × delta time
- Particles are drawn as small rotated rectangles using `canvas.save()`, `canvas.rotate()`, `canvas.drawRect()`, `canvas.restore()`
- Animation loop driven by repeated `invalidate()` calls; terminates automatically when all particles have decayed or left the screen bounds

---

## Audio System

`GameSoundManager` manages all game audio using two separate Android audio APIs:

| API | Used For | Resource |
|---|---|---|
| `MediaPlayer` | Looping background music at 50% volume | `res/raw/bgm_game.mp3` |
| `SoundPool` | Short sound effects (up to 4 concurrent streams) | `.wav` files |

`SoundPool` is configured with `AudioAttributes.USAGE_GAME` and `CONTENT_TYPE_SONIFICATION`.

**Sound effects:**

| Event | Method | File |
|---|---|---|
| Correct gesture matched | `playMatch()` | `sound_correct.wav` |
| Round timeout or shake skip | `playFailed()` | `sound_failed.wav` |
| Game completed | `playGameOver()` | `sound_game_complete.wav` |

On game over, the background music is stopped before the completion sound plays. All resources (`MediaPlayer`, `SoundPool`) are released in `release()` when the game session ends.

---

## Data Layer — Room Database

Persistent data is managed by **Room** (`androidx.room:room-runtime:2.6.1`), Android's SQLite abstraction library. KSP (Kotlin Symbol Processing) generates DAO implementations at compile time.

### Database: `AppDatabase`

A thread-safe singleton accessed via double-checked locking with `@Volatile`:

```kotlin
@Database(entities = [UserEntity::class, ScoreEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase()
```

The database file is named `hole_in_the_wall.db`. Schema export is enabled (`exportSchema = true`) for version-control auditing.

### Table 1: `users` (UserEntity)

| Column | Type | Constraint |
|---|---|---|
| `id` | `LONG` | Primary key, auto-generated |
| `username` | `TEXT` | Unique index |
| `passwordHash` | `TEXT` | SHA-256 hex digest, never plaintext |
| `avatarResId` | `INT` | Optional drawable resource ID |
| `createdAt` | `LONG` | Unix timestamp (ms) |

### Table 2: `scores` (ScoreEntity)

| Column | Type | Constraint |
|---|---|---|
| `id` | `LONG` | Primary key, auto-generated |
| `userId` | `LONG` | Foreign key → `users.id` with `CASCADE DELETE` |
| `score` | `INT` | Final game score |
| `wallsCleared` | `INT` | Number of correctly matched gestures |
| `durationMs` | `LONG` | Session duration in milliseconds |
| `recordedAt` | `LONG` | Unix timestamp (ms) of session end |

The foreign key with `onDelete = CASCADE` ensures that if a user account is deleted, all their associated score records are automatically removed.

### ScoreDao — Key Queries

```sql
-- Personal best for a single user
SELECT MAX(score) FROM scores WHERE userId = :userId

-- Global leaderboard — JOIN across both tables
SELECT u.username, s.score, s.wallsCleared, s.recordedAt
FROM scores s
INNER JOIN users u ON s.userId = u.id
ORDER BY s.score DESC
LIMIT :limit
```

Both `getLeaderboard()` and `getScoresForUser()` return `Flow<List<...>>` so they automatically push updated results to all active observers whenever the underlying table changes — no polling required.

### Score Saving Flow

When a game ends, `CameraFragment.onGameOver()` calls `mainViewModel.saveGameScore(score, correctCount)`. The ViewModel creates a `ScoreEntity` and dispatches `repository.saveScore()` on `Dispatchers.IO`. After saving, `refreshPersonalBest()` is called to immediately update the Home screen's displayed personal best.

---

## Leaderboard

`LeaderboardFragment` displays the top 10 global scores in a `RecyclerView`.

`LeaderboardViewModel` exposes the leaderboard as a `StateFlow<List<LeaderboardEntry>>` using `stateIn()` with a 5-second subscription timeout to avoid unnecessary database observation when the screen is not visible:

```kotlin
val leaderboard: StateFlow<List<LeaderboardEntry>> =
    repository.getLeaderboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

`LeaderboardAdapter` uses `DiffUtil.ItemCallback` for efficient `RecyclerView` updates without full list redraws. The top 3 entries are styled with distinct visual treatment:

| Rank | Emoji | Colour |
|---|---|---|
| 1st | 🥇 | Gold `#FFD700` |
| 2nd | 🥈 | Silver `#C0C0C0` |
| 3rd | 🥉 | Bronze `#CD7F32` |
| 4th+ | `#N` | Purple-tinted `#6060A0` |

An empty state view is shown when no scores have been recorded yet.

---

## Technology Stack & Dependencies

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | 1.9.x | Primary language |
| Android SDK | `minSdk 24`, `targetSdk 34` | Platform target |
| CameraX | 1.3.1 | Camera preview, capture, and frame analysis |
| Google ML Kit Face Detection | 16.1.6 | Real-time face and landmark detection |
| Google MediaPipe Tasks Vision | 0.10.9 | Hand gesture recognition (21-point landmark model) |
| Room | 2.6.1 | SQLite ORM with multi-table relational schema |
| Jetpack Navigation | 2.7.6 | Fragment navigation with bottom nav bar |
| Jetpack Compose + Material3 | BOM 2024.05.00 | Login / Register screen UI |
| Kotlin Coroutines + Flow | (via Lifecycle 2.7.0) | Async data access and reactive UI |
| ViewBinding | Built-in | Type-safe XML view access in Fragments |
| KSP | Bundled | Compile-time Room DAO code generation |
| Coil | 2.5.0 | Photo gallery image loading |
| Material Design Components | 1.11.0 | Bottom sheets, navigation bar, chips |

---

## Permissions

| Permission | Reason |
|---|---|
| `CAMERA` | Camera preview, face detection, and gesture recognition |
| `READ_MEDIA_IMAGES` (API 33+) | Access photos from MediaStore for the in-app gallery |
| `READ_EXTERNAL_STORAGE` (API ≤ 32) | Legacy storage permission for gallery on older devices |
| `INTERNET` | Declared (reserved for potential future cloud features) |

Camera permission is requested at runtime using `ActivityResultContracts.RequestPermission()`. If denied, a permission-message UI is shown with a manual request button. Storage permission is requested on demand only when the gallery is opened.

The app declares `android.hardware.camera` as **not required** (`android:required="false"`) so it can be installed on devices without a camera, though camera-dependent features will be unavailable on such devices.
