# SlowDown 慢一点

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="SlowDown Logo"/>
  
  **A privacy-first digital mindfulness app for Android**
  
  *帮助用户养成更健康的智能手机使用习惯*

  [![Android](https://img.shields.io/badge/Android-SDK%2034-green)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-purple)](https://developer.android.com/jetpack/compose)
  [![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
</div>

---

## ✨ Features

### 🧘 Mindful Intervention
- **Breathing Pause**: Shows a calming breathing animation when opening monitored apps
- **Gentle Reminders**: Non-intrusive prompts to reflect before continuing
- **Strict Limits**: Enforce daily time limits with automatic app blocking

### 📊 Usage Insights
- **Daily Statistics**: Track screen time per app with beautiful charts
- **Weekly Trends**: Visualize usage patterns over time
- **Intervention Counter**: See how many times you've been protected

### 🔒 Privacy First
- **100% Local Storage**: All data stays on your device
- **No Cloud Sync**: No accounts, no servers, no tracking
- **Open Source**: Fully transparent codebase

### 🌍 Bilingual Support
- **中文 / English**: Full internationalization support
- **Runtime Switching**: Change language without restart

---

## 📱 Screenshots

| Dashboard | App List | Statistics |
|:---------:|:--------:|:----------:|
| Breathing Circle | Protected Apps | Usage Charts |

---

## 🚀 Quick Start

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Android Studio | Hedgehog (2023.1.1)+ |
| JDK | 17+ |
| Android SDK | 34 |
| Kotlin | 2.0+ |

### Build & Install

```bash
# Clone the repository
git clone https://github.com/originem0/SlowDown.git
cd SlowDown

# Build Debug APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug
```

### Required Permissions

After installation, grant the following permissions:

| Permission | Location | Purpose |
|------------|----------|---------|
| **Accessibility Service** | Settings → Accessibility → SlowDown | Monitor app launches |
| **Display Over Other Apps** | Settings → Apps → SlowDown → Display over other apps | Show breathing overlay |
| **Usage Access** | Settings → Security → Usage access → SlowDown | Read app usage time |

---

## 📖 Usage Guide

### Adding Apps to Monitor

1. Open SlowDown
2. Navigate to the **Apps** tab
3. Tap **+** on any app in the "Available" list
4. Tap the app to configure its restriction mode

### Restriction Modes

| Mode | Behavior |
|------|----------|
| **Tracking Only** | Records usage time silently |
| **Gentle Reminder** | Shows breathing pause, user can dismiss |
| **Strict Mode** | Enforces daily limit, blocks when exceeded |

### Time Limits

- Set daily limits (in minutes) per app
- Warning at 80% usage
- Enforcement at 100% based on mode

---

## 🏗️ Architecture

```
app/src/main/java/com/example/slowdown/
├── data/
│   ├── local/
│   │   ├── dao/           # Room DAO interfaces
│   │   ├── entity/        # Data entities
│   │   └── AppDatabase.kt # Room database
│   ├── preferences/       # DataStore preferences
│   └── repository/        # Data repositories
├── service/
│   ├── AppMonitorService.kt    # AccessibilityService (core)
│   ├── OverlayService.kt       # Breathing overlay
│   └── UsageTrackingManager.kt # Usage time tracking
├── ui/
│   ├── components/        # Reusable Compose components
│   ├── navigation/        # NavGraph
│   ├── overlay/           # Overlay Activity
│   ├── screen/            # Main screens
│   └── theme/             # Material 3 theming
├── viewmodel/             # ViewModels
└── util/                  # Utility classes
```

### Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository Pattern |
| Database | Room |
| Async | Kotlin Coroutines + Flow |
| DI | Manual (Application-level singletons) |

### Key Design Decisions

#### Passive Event Triggering
The overlay is triggered by `AccessibilityEvent` rather than polling:
- Battery efficient
- Non-invasive
- Only intervenes on user action

#### Three-Layer Foreground Verification
Prevents false positives:
1. **Event Reception**: Clears tracking when switching to SlowDown/system apps
2. **Warning Check**: Validates via `rootInActiveWindow`
3. **Pre-Overlay**: Final foreground verification

---

## 🎨 Design Language

### Visual Identity
- **Breathing Circle**: 320dp animated orb with multi-layer gradients
- **Skeuomorphic Depth**: Soft shadows and glows for tactile feel
- **Embedded Flat**: Inset search bars and minimal cards

### Color Palette
- **Primary**: Teal/Cyan (Focus, Calm)
- **Secondary**: Warm Sand (Comfort)
- **Surface**: Neutral Grays (Readability)

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Function Spec](docs/function.md) | Detailed feature documentation |
| [Popup Logic](docs/popup-logic-flowchart.md) | Overlay trigger mechanism |
| [i18n Plan](docs/plans/2025-01-17-remaining-screens-i18n.md) | Internationalization implementation |

---

## 🛠️ Development Notes

### Atomic State Updates
Always use `updateRestrictionMode()` for multi-field changes:
```kotlin
// ✅ Correct
viewModel.updateRestrictionMode(mode, limit)

// ❌ Incorrect
viewModel.setMode(mode)
viewModel.setLimit(limit)
```

### CJK Search Normalization
Handle full-width characters from IME:
```kotlin
val normalized = query.lowercase(Locale.ROOT).toHalfWidth()
```

### AccessibilityService Configuration
- Configure in `accessibility_service_config.xml`
- Use `rootInActiveWindow` for foreground detection
- Handle null cases gracefully

---

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Submit a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <sub>Built with 💚 for digital wellness</sub>
</div>
