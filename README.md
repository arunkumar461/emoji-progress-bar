# Emoji Progress Bar Plugin

Emoji Progress Bar is a lightweight IntelliJ Platform plugin (compatible with IntelliJ IDEA, WebStorm, and other JetBrains IDEs) that replaces the default task progress indicator with a customizable emoji-driven animation. The experience is inspired by the classic Nyan progress bar but lets you bring your own flair by selecting any emoji sequence.

## Features

- Renders progress bars with a rounded, colorful track and animated emoji trail.
- Choose any emoji sequence (single or space separated) for the active portion of the bar.
- Pick a track filler glyph and tune the indeterminate animation speed.
- Ships with Settings / Preferences UI under `Appearance & Behavior → Emoji Progress Bar`.

## Project Layout

```
.
├── build.gradle.kts                # Gradle IntelliJ plugin configuration
├── gradle/                         # Gradle wrapper configuration
├── src/main/kotlin/                # Kotlin sources
│   ├── com/madesha/emoji/progress/  # Startup listener
│   ├── .../settings/                # Persistent settings + configurable UI
│   └── .../ui/                      # Progress bar UI + installer
└── src/main/resources/META-INF/    # Plugin metadata (plugin.xml)
```

## Getting Started

1. **Generate the Gradle wrapper JAR (first time only).**  
   If the `gradle/wrapper/gradle-wrapper.jar` file is missing, run `gradle wrapper --gradle-version 8.5` once using a locally installed Gradle distribution.

2. **Build or run the plugin:**
   ```bash
   ./gradlew build           # assemble the plugin ZIP under build/distributions
   ./gradlew runIde          # launch a sandbox IDE with the plugin loaded
   ```

3. **Install in your IDE:** install the generated ZIP via `Settings → Plugins → ⚙️ → Install Plugin from Disk…`.

## Customizing the Progress Bar

- Open `Settings / Preferences → Appearance & Behavior → Emoji Progress Bar`.
- Enter an emoji sequence (separate multiple emoji with spaces for cycling).
- Choose a fallback character for the track and adjust indeterminate animation speed.
- Apply the changes; active progress indicators update immediately.

## Notes

- Requires an IDE build `2023.3` or newer (sinceBuild `233`).
- The custom UI automatically restores the original look when the plugin is disabled or unloaded.
- For additional styling tweaks, adjust `EmojiProgressBarUi` in `src/main/kotlin/com/madesha/emoji/progress/ui/`.

