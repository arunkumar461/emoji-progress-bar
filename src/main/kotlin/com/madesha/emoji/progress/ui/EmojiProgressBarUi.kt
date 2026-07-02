package com.madesha.emoji.progress.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.madesha.emoji.progress.settings.EmojiProgressBarSettings
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class EmojiProgressBarUi : BasicProgressBarUI() {

    override fun installDefaults() {
        super.installDefaults()
        progressBar.border = JBUI.Borders.empty(2)
        progressBar.isBorderPainted = false
    }

    override fun getPreferredSize(c: JComponent?): java.awt.Dimension {
        val size = super.getPreferredSize(c)
        size.height = max(size.height, desiredHeight())
        return size
    }

    override fun getMinimumSize(c: JComponent?): java.awt.Dimension {
        val size = super.getMinimumSize(c)
        size.height = max(size.height, desiredHeight())
        return size
    }

    override fun paintDeterminate(g: Graphics, c: JComponent) {
        paintEmojiProgress(g, c, computeFractionOverride = null)
    }

    override fun paintIndeterminate(g: Graphics, c: JComponent) {
        val settings = EmojiProgressBarSettings.getInstance().state
        val speed = settings.indeterminateSpeedMs.coerceIn(MIN_ANIMATION_MS, MAX_ANIMATION_MS)
        val cycle = speed * INDETERMINATE_STEPS
        val now = System.currentTimeMillis()
        val fraction = ((now % cycle).toDouble() / cycle).coerceIn(0.0, 1.0)
        paintEmojiProgress(g, c, fraction)
    }

    private fun paintEmojiProgress(g: Graphics, c: JComponent, computeFractionOverride: Double?) {
        val bar = progressBar
        val state = EmojiProgressBarSettings.getInstance().state
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        // GASP respects each font's own hinting tables — correct for color emoji bitmap fonts
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)

        val insets = bar.insets
        val width = bar.width - insets.left - insets.right
        val height = bar.height - insets.top - insets.bottom
        if (width <= 0 || height <= 0) {
            g2.dispose()
            return
        }

        g2.translate(insets.left, insets.top)

        val radius = max(8, ceil(height * 0.8).toInt())
        val trackBackground = colorFromHex(
            state.trackColorHex,
            EmojiProgressBarSettings.DEFAULT_TRACK_COLOR,
            DEFAULT_TRACK_COLOR
        )
        val progressFill = colorFromHex(
            state.progressColorHex,
            EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR,
            DEFAULT_PROGRESS_COLOR
        )
        val borderColor = colorFromHex(
            state.borderColorHex,
            EmojiProgressBarSettings.DEFAULT_BORDER_COLOR,
            DEFAULT_BORDER_COLOR
        )

        val shape = RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), radius.toFloat(), radius.toFloat())
        g2.color = trackBackground
        g2.fill(shape)

        val fraction = (computeFractionOverride ?: bar.percentComplete).coerceIn(0.0, 1.0)
        val progressWidth = max(1, (width * fraction).toInt())
        if (fraction > 0.0) {
            val progressShape = RoundRectangle2D.Float(
                0f,
                0f,
                progressWidth.toFloat(),
                height.toFloat(),
                radius.toFloat(),
                radius.toFloat()
            )
            g2.color = progressFill
            g2.fill(progressShape)
        }

        g2.color = borderColor
        g2.draw(shape)

        val indicatorScale = indicatorScale(state)
        paintIndicator(g2, width, height, fraction, progressWidth, state, indicatorScale, System.currentTimeMillis())

        g2.dispose()
    }

    private fun paintIndicator(
        g2: Graphics2D,
        width: Int,
        height: Int,
        fraction: Double,
        progressWidth: Int,
        state: EmojiProgressBarSettings.State,
        scale: Double,
        timestamp: Long
    ) {
        val indicatorImage = loadIndicatorImage(state)
        if (indicatorImage != null) {
            val targetHeight = (height * scale).roundToInt().coerceAtLeast(JBUI.scale(16)).coerceAtMost(height)
            val imageScale = targetHeight.toDouble() / indicatorImage.height.coerceAtLeast(1)
            val targetWidth = (indicatorImage.width * imageScale).roundToInt().coerceAtLeast(JBUI.scale(16)).coerceAtMost(width)

            val clampedProgress = progressWidth.coerceAtMost(width)
            val availableWidth = (width - targetWidth).coerceAtLeast(0)
            val centerX = clampedProgress - targetWidth / 2
            val x = centerX.coerceIn(0, availableWidth)
            val y = ((height - targetHeight) / 2).coerceAtLeast(0)

            g2.drawImage(indicatorImage, x, y, targetWidth, targetHeight, null)
            return
        }

        val emojiTokens = parseEmojiTokens(state.emojiSequence).ifEmpty {
            listOf(EmojiProgressBarSettings.DEFAULT_EMOJI_SEQUENCE)
        }

        val emoji = if (emojiTokens.size == 1) {
            emojiTokens.first()
        } else {
            val animationStepMs = state.indeterminateSpeedMs.coerceIn(60, 400).toLong()
            val phase = ((timestamp / animationStepMs) % emojiTokens.size).toInt()
            emojiTokens[phase]
        }

        // Size the font from the actual bar pixel height so the emoji fills the bar
        val targetPt = (height * scale).toFloat().coerceIn(JBUI.scale(14).toFloat(), JBUI.scale(56).toFloat())
        val emojiFont = findEmojiFont(targetPt)
        val savedFont = g2.font
        g2.font = emojiFont

        try {
            // TextLayout performs full Unicode shaping (ZWJ sequences, skin-tone modifiers)
            // and gives accurate glyph bounds — drawString() does neither on Linux/JVM
            val layout = TextLayout(emoji, g2.font, g2.fontRenderContext)
            val bounds = layout.bounds
            val emojiWidth = bounds.width.toInt().coerceAtLeast(JBUI.scale(16))
            val availableWidth = (width - emojiWidth).coerceAtLeast(0)

            val centerX = progressWidth - emojiWidth / 2
            val emojiX = centerX.coerceIn(0, availableWidth)

            // bounds.y is the ascent offset (negative = above baseline), so subtract it to centre vertically
            val baseline = ((height - bounds.height) / 2 - bounds.y).toInt()

            g2.color = UIUtil.getLabelForeground()
            layout.draw(g2, emojiX.toFloat(), baseline.toFloat())
        } catch (_: Exception) {
            // Fallback for fonts that can't shape this emoji: plain drawString at a safe position
            val fm = g2.fontMetrics
            val emojiWidth = fm.stringWidth(emoji).coerceAtLeast(JBUI.scale(16))
            val availableWidth = (width - emojiWidth).coerceAtLeast(0)
            val centerX = progressWidth - emojiWidth / 2
            val emojiX = centerX.coerceIn(0, availableWidth)
            val baseline = ((height + fm.ascent - fm.descent) / 2)
            g2.color = UIUtil.getLabelForeground()
            g2.drawString(emoji, emojiX, baseline)
        }

        g2.font = savedFont
    }

    private fun parseEmojiTokens(raw: String): List<String> =
        raw.split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun colorFromHex(hex: String?, fallbackHex: String, fallback: JBColor): Color {
        if (hex.isNullOrBlank()) return fallback
        val cleaned = hex.removePrefix("#")
        if (cleaned.equals(fallbackHex, ignoreCase = true)) {
            return fallback
        }
        return try {
            val value = cleaned.toInt(16)
            Color(value shr 16 and 0xFF, value shr 8 and 0xFF, value and 0xFF)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun loadIndicatorImage(state: EmojiProgressBarSettings.State): BufferedImage? {
        if (!state.useImageIndicator) return null
        val path = state.imagePath.trim()
        if (path.isEmpty()) return null
        return loadImageFromPath(path)
    }

    private fun indicatorScale(state: EmojiProgressBarSettings.State): Double =
        state.indicatorScalePercent.coerceIn(
            EmojiProgressBarSettings.MIN_INDICATOR_SCALE_PERCENT,
            EmojiProgressBarSettings.MAX_INDICATOR_SCALE_PERCENT
        ) / 100.0

    private fun desiredHeight(): Int {
        val state = EmojiProgressBarSettings.getInstance().state
        val scale = indicatorScale(state)
        val base = JBUI.scale(24)
        return (base * scale).roundToInt()
            .coerceAtLeast(JBUI.scale(24))
            .coerceAtMost(JBUI.scale(80))
    }

    companion object {
        private const val INDETERMINATE_STEPS = 24L
        private const val MIN_ANIMATION_MS = 45
        private const val MAX_ANIMATION_MS = 600

        private val DEFAULT_TRACK_COLOR = JBColor(Color(0xF2, 0xF4, 0xF9), Color(0x2B, 0x2D, 0x32))
        private val DEFAULT_PROGRESS_COLOR = JBColor(Color.WHITE, Color(0x3B, 0x40, 0x48))
        private val DEFAULT_BORDER_COLOR = JBColor(Color(0xD0, 0xD4, 0xE0), Color(0x43, 0x46, 0x4E))

        private data class CachedImage(val timestamp: Long, val image: BufferedImage?)
        private val imageCache = ConcurrentHashMap<String, CachedImage>()

        // Cache resolved emoji font to avoid repeated font lookup on every paint call
        @Volatile private var cachedEmojiFont: Font? = null

        val UI_CLASS_NAME: String = EmojiProgressBarUi::class.java.name

        @JvmStatic
        fun createUI(component: JComponent?): BasicProgressBarUI = EmojiProgressBarUi()

        /**
         * Finds the best available color-emoji font on this system.
         * Java2D does NOT automatically fall back to an emoji font, so we must select one explicitly.
         * Priority: platform-native color emoji fonts, then generic fallback.
         */
        fun findEmojiFont(sizePt: Float): Font {
            cachedEmojiFont?.let { return it.deriveFont(sizePt) }

            val candidates = listOf(
                "Noto Color Emoji",   // Linux (most common)
                "Segoe UI Emoji",     // Windows
                "Apple Color Emoji",  // macOS
                "EmojiOne Mozilla",   // Firefox bundles
                "Twemoji Mozilla",
                "Symbola"             // fallback coverage font
            )
            val available = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .availableFontFamilyNames.toHashSet()
            val resolved = candidates.firstOrNull { it in available }
                ?: candidates.firstOrNull { name ->
                    // Font(String,...) silently falls back to Dialog when the family is missing;
                    // confirm by checking the returned family name
                    Font(name, Font.PLAIN, 16).family.equals(name, ignoreCase = true)
                }

            val font = if (resolved != null) {
                Font(resolved, Font.PLAIN, sizePt.toInt())
            } else {
                // No dedicated emoji font found — use the default font and hope for OS-level fallback
                Font(Font.SANS_SERIF, Font.PLAIN, sizePt.toInt())
            }
            cachedEmojiFont = font
            return font.deriveFont(sizePt)
        }

        private fun loadImageFromPath(path: String): BufferedImage? {
            val file = File(path)
            val key = file.absolutePath
            if (!file.exists() || !file.isFile) {
                imageCache.remove(key)
                return null
            }
            val timestamp = file.lastModified()
            imageCache[key]?.let { cached ->
                if (cached.timestamp == timestamp) return cached.image
            }
            val image = try { ImageIO.read(file) } catch (_: Exception) { null }
            imageCache[key] = CachedImage(timestamp, image)
            return image
        }
    }
}
