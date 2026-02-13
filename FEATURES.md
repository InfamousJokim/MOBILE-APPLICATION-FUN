# Detection Features

## Hand Gesture Recognition

Powered by **MediaPipe Gesture Recognizer** with real-time live stream processing.
Supports detection of up to **2 hands** simultaneously.

| Gesture | Internal Name | Description |
|---------|---------------|-------------|
| Fist | `Closed_Fist` | All fingers curled into a closed fist |
| Open Palm | `Open_Palm` | Hand open with all five fingers spread out |
| Pointing Up | `Pointing_Up` | Index finger extended and pointing upward |
| Thumbs Up | `Thumb_Up` | Thumb extended upward, other fingers closed |
| Thumbs Down | `Thumb_Down` | Thumb extended downward, other fingers closed |
| Peace Sign | `Victory` | Index and middle fingers raised in a V shape |
| I Love You | `ILoveYou` | ASL sign with thumb, index finger, and pinky extended |

### Hand Landmark Tracking

Each detected hand is rendered with **21 landmarks** connected by **23 skeletal connections** covering:

- **Thumb** (4 joints)
- **Index finger** (4 joints)
- **Middle finger** (4 joints)
- **Ring finger** (4 joints)
- **Pinky finger** (4 joints)
- **Wrist** (base connection point)

---

## Facial Feature Detection

Powered by **Google ML Kit Face Detection** with fast performance mode.

### Facial Landmarks

| Landmark | Description |
|----------|-------------|
| Left Eye | Center position of the left eye |
| Right Eye | Center position of the right eye |
| Nose Base | Base/bridge of the nose |
| Mouth Left | Left corner of the mouth |
| Mouth Right | Right corner of the mouth |
| Mouth Bottom | Bottom center of the mouth |
| Left Ear | Left ear position |
| Right Ear | Right ear position |
| Left Cheek | Left cheek position |
| Right Cheek | Right cheek position |

### Facial Expressions & Head Orientation

| Feature | Detection Criteria | Display Label |
|---------|-------------------|---------------|
| Smiling | Smile probability > 50% | `Smiling` |
| Eyes Closed | Both eyes open probability < 30% | `Eyes Closed` |
| Looking Left | Head Y-axis rotation > 20 degrees | `Looking Left` |
| Looking Right | Head Y-axis rotation < -20 degrees | `Looking Right` |

### Face Detection Settings

- **Minimum face size:** 15% of image
- **Face tracking:** Enabled (persistent face IDs across frames)
- **Bounding box:** Green outline drawn around each detected face
- **Landmarks:** Yellow dots rendered at each facial landmark point
