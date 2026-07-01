# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build the plugin ZIP (output: build/distributions/emoji-progress-bar-0.1.0.zip)
./gradlew build

# Launch a sandbox IDE with the plugin loaded for manual testing
./gradlew runIde

# Run tests only
./gradlew test

# First-time setup if gradle-wrapper.jar is missing
gradle wrapper --gradle-version 8.7
```

Install the built ZIP via `Settings → Plugins → ⚙️ → Install Plugin from Disk…`.

## Architecture

This is an **IntelliJ Platform plugin** (Kotlin/JVM, Gradle build, targeting IC 2024.3+). There are no tests — the plugin is verified manually via `runIde`.

### Entry point and lifecycle

`EmojiProgressBarStartupActivity` (registered as a `postStartupActivity` in `plugin.xml`) runs on project open and calls `EmojiProgressBarManager.initialize()`. The manager is an application-level `@Service`.

### UI installation (`EmojiProgressBarManager`)

Installs the custom progress bar by overriding two keys in both `UIManager.getDefaults()` and `UIManager.getLookAndFeelDefaults()`:
- `"ProgressBarUI"` → the UI class name
- The UI class name itself → the `EmojiProgressBarUi` class

Captures the originals before installing and restores them in `dispose()`, so the IDE reverts cleanly when the plugin is disabled. Also subscribes to `LafManagerListener` to re-install after look-and-feel changes, and to `EmojiProgressBarSettings.TOPIC` to force-repaint all open progress bars on settings change.

### Rendering (`EmojiProgressBarUi`)

Extends `BasicProgressBarUI`. Paints a rounded-rect track, a filled progress region, a border, then overlays either:
- A custom image (loaded lazily with timestamp-based cache in `imageCache`)
- An animated emoji drawn as a string, cycling through space-separated tokens from `emojiSequence` using `(timestamp / speedMs) % tokens.size` for the phase

`paintIndeterminate` derives a `fraction` from `System.currentTimeMillis() % (speed * 24)` to drive the animation.

### Settings (`EmojiProgressBarSettings`)

Application-level `PersistentStateComponent` backed by `emojiProgressBarSettings.xml`. Exposes a `State` data class and an `update {}` lambda that normalizes and persists state, then fires `TOPIC` on the application message bus.

### Settings UI (`EmojiProgressBarConfigurable` + `EmojiProgressBarSettingsComponent`)

`EmojiProgressBarConfigurable` implements `SearchableConfigurable` (standard IntelliJ settings page) and delegates to `EmojiProgressBarSettingsComponent` for the Swing form. The component shows a live preview panel that re-renders on every field change without saving.

### Plugin registration (`plugin.xml`)

Declares the `applicationConfigurable` extension and the `postStartupActivity`. No application service declarations — the `@Service` annotation on the Kotlin classes is sufficient for IntelliJ Platform 2.x Gradle plugin.
