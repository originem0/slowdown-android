# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- Custom reminder text setting - users can add personal motivational messages
- Per-app cooldown settings - individual apps can have custom cooldown periods
- **About page** - new settings page with version info, developer contact, links, and legal information

### Changed
- Removed OverlayService, unified overlay launch to use OverlayActivity directly
- Optimized overlay rendering to eliminate splash screen flash

### Fixed
- Improved stability and code quality (database downgrade protection, memory management)
- **Bug fix #1**: Service toggle now properly stops all overlay popups when disabled
- **Bug fix #3**: Cooldown maps are now cleared when service is disabled (clean state on re-enable)
- **Bug fix #4**: Realtime tracking buffer is flushed before sync to prevent data loss
- **Bug fix #5**: Fixed Handler memory leak in AppMonitorService
- **Bug fix #6**: Fixed infinite loop coroutine leak (proper isActive check)
- **Bug fix #7**: Added try-catch for app launch to prevent crashes
- **Bug fix #8**: Removed runBlocking ANR risk in UsageWarningActivity (use cached language)

---

## [1.0.0] - 2026-01-18

### Added
- **Core Features**
  - Deep breath intervention popup with countdown timer
  - Flexible restriction modes: soft reminder vs strict blocking
  - Daily usage time limits with 80%/100% warning thresholds
  - Redirect to alternative apps (e.g., reading apps)
  - Short video mode for TikTok/Douyin type apps (30-second active check)

- **Usage Tracking**
  - Real-time usage time tracking via UsageStatsManager
  - Dynamic sync intervals (5min default, 1min when approaching limits)
  - Daily/weekly/monthly statistics dashboard

- **User Experience**
  - Breathing animation circle UI
  - Material 3 design with bilingual support (English/Chinese)
  - Onboarding flow for permission setup
  - Success rate tracking

- **Compatibility**
  - MIUI foreground service to prevent AccessibilityService freeze
  - Samsung One UI compatibility
  - Full-screen video detection (rootInActiveWindow null handling)

### Technical Details
- Database version: 4 (with migrations from v1)
- Minimum SDK: Android 6.0 (API 23)
- Target SDK: Android 14 (API 34)

---

## Development History

### 2026-01-23
- `fix: comprehensive bug fixes for service toggle, memory leaks, and ANR risks`
- `feat: add About page with developer info and legal links`

### 2026-01-20
- `fix: improve stability and code quality`
- `refactor: remove OverlayService and unify overlay launch strategy`
- `perf: optimize overlay rendering and eliminate splash screen flash`
- `feat: add custom reminder text setting`
- `feat: add per-app cooldown setting`

### 2026-01-19
- `docs: add monitoring optimization design document`

### 2026-01-18
- `docs: add screenshots to README`
- `build: add release signing config`
- `chore: update launcher icons and minor UI tweaks`
- `chore: add MIT LICENSE`
- `feat: One UI compatibility + success rate + onboarding optimization`

### 2026-01-17
- `ui: Remove stats card from AppListScreen`
- `docs: Comprehensive README update`
- `Phase 3: UI/UX Pro Max Refinement - i18n, Dashboard Breathing Circle, AppList Hierarchy`
- `fix: rootInActiveWindow null handling + short video mode database migration`
- `feat: add UsageStats permission handling`
- `feat: add Statistics screen with bottom navigation`
- `feat: add usage warning dialogs for time limit alerts`
- `feat: add usage time tracking with realtime monitoring`
- `fix: replace SimpleDateFormat with LocalDate for thread safety`

---

## Feature Roadmap

### Planned
- [ ] Time period restrictions (e.g., 22:00-06:00 only)
- [ ] Weekly total usage limits
- [ ] Text matching challenge mode
- [ ] Image viewing task mode
- [ ] Achievement system
- [ ] Data export/backup
- [ ] Cloud sync (optional)
- [ ] Machine learning for optimal intervention timing

### Under Consideration
- [ ] UsageStatsManager fallback when rootInActiveWindow is null
- [ ] Battery-aware mode (reduce checks when low battery)
- [ ] Geofencing support (location-based restrictions)
