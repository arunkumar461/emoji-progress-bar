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
import java.awt.geom.RoundRectangle2D
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
        paintEmojiProgress(g, c, fractionOverride = null)
    }

    override fun paintIndeterminate(g: Graphics, c: JComponent) {
        val settings = EmojiProgressBarSettings.getInstance().state
        val speed = settings.indeterminateSpeedMs.coerceIn(MIN_ANIMATION_MS, MAX_ANIMATION_MS)
        val cycle = speed * INDETERMINATE_STEPS
        val fraction = ((System.currentTimeMillis() % cycle).toDouble() / cycle).coerceIn(0.0, 1.0)
        paintEmojiProgress(g, c, fractionOverride = fraction)
    }

    private fun paintEmojiProgress(g: Graphics, c: JComponent, fractionOverride: Double?) {
        val bar = progressBar
        val state = EmojiProgressBarSettings.getInstance().state
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)

        val insets = bar.insets
        val width = bar.width - insets.left - insets.right
        val height = bar.height - insets.top - insets.bottom
        if (width <= 0 || height <= 0) { g2.dispose(); return }

        g2.translate(insets.left, insets.top)

        val radius = max(8, ceil(height * 0.8).toInt()).toFloat()
        val shape = RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), radius, radius)

        g2.color = colorFromHex(state.trackColorHex, EmojiProgressBarSettings.DEFAULT_TRACK_COLOR, DEFAULT_TRACK_COLOR)
        g2.fill(shape)

        val fraction = (fractionOverride ?: bar.percentComplete).coerceIn(0.0, 1.0)
        val progressWidth = max(1, (width * fraction).toInt())
        if (fraction > 0.0) {
            g2.color = colorFromHex(state.progressColorHex, EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR, DEFAULT_PROGRESS_COLOR)
            g2.fill(RoundRectangle2D.Float(0f, 0f, progressWidth.toFloat(), height.toFloat(), radius, radius))
        }

        g2.color = colorFromHex(state.borderColorHex, EmojiProgressBarSettings.DEFAULT_BORDER_COLOR, DEFAULT_BORDER_COLOR)
        g2.draw(shape)

        paintEmoji(g2, width, height, progressWidth, state, System.currentTimeMillis())

        g2.dispose()
    }

    private fun paintEmoji(
        g2: Graphics2D,
        width: Int,
        height: Int,
        progressWidth: Int,
        state: EmojiProgressBarSettings.State,
        timestamp: Long
    ) {
        val tokens = state.emojiSequence
            .split(" ").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf(EmojiProgressBarSettings.DEFAULT_EMOJI_SEQUENCE) }

        val emoji = if (tokens.size == 1) {
            tokens.first()
        } else {
            val stepMs = state.indeterminateSpeedMs.coerceIn(60, 400).toLong()
            tokens[((timestamp / stepMs) % tokens.size).toInt()]
        }

        // 65% of bar height keeps the glyph safely inside the track on all platforms
        val targetPt = (height * 0.65f).coerceIn(JBUI.scale(8).toFloat(), JBUI.scale(32).toFloat())
        val emojiFont = findEmojiFont(targetPt)
        val savedFont = g2.font
        val savedClip = g2.clip
        g2.font = emojiFont
        // Hard clip so any platform-specific font overflow never escapes the bar bounds
        g2.setClip(0, 0, width, height)

        val fm = g2.fontMetrics
        val emojiWidth = fm.stringWidth(emoji).coerceAtLeast(JBUI.scale(16))
        val emojiX = (progressWidth - emojiWidth / 2).coerceIn(0, (width - emojiWidth).coerceAtLeast(0))
        // Standard baseline: centres the ascent+descent block within the bar height
        val baseline = (height + fm.ascent - fm.descent) / 2
        g2.color = UIUtil.getLabelForeground()
        g2.drawString(emoji, emojiX, baseline)

        g2.font = savedFont
        g2.clip = savedClip
    }

    private fun colorFromHex(hex: String?, fallbackHex: String, fallback: JBColor): Color {
        if (hex.isNullOrBlank()) return fallback
        val cleaned = hex.removePrefix("#")
        if (cleaned.equals(fallbackHex, ignoreCase = true)) return fallback
        return try {
            val v = cleaned.toInt(16)
            Color(v shr 16 and 0xFF, v shr 8 and 0xFF, v and 0xFF)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun desiredHeight(): Int = JBUI.scale(24)

    companion object {
        private const val INDETERMINATE_STEPS = 24L
        private const val MIN_ANIMATION_MS = 45
        private const val MAX_ANIMATION_MS = 600

        private val DEFAULT_TRACK_COLOR = JBColor(Color(0xFF, 0xF3, 0xCD), Color(0x3D, 0x30, 0x10))
        private val DEFAULT_PROGRESS_COLOR = JBColor(Color(0xFF, 0xD1, 0x66), Color(0x7A, 0x55, 0x10))
        private val DEFAULT_BORDER_COLOR = JBColor(Color(0xF0, 0xA5, 0x00), Color(0xC0, 0x80, 0x00))

        @Volatile private var cachedEmojiFont: Font? = null

        val UI_CLASS_NAME: String = EmojiProgressBarUi::class.java.name

        @JvmStatic
        fun createUI(component: JComponent?): BasicProgressBarUI = EmojiProgressBarUi()

        fun findEmojiFont(sizePt: Float): Font {
            cachedEmojiFont?.let { return it.deriveFont(sizePt) }

            val candidates = listOf(
                "Noto Color Emoji",
                "Segoe UI Emoji",
                "Apple Color Emoji",
                "EmojiOne Mozilla",
                "Twemoji Mozilla",
                "Symbola"
            )
            val available = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .availableFontFamilyNames.toHashSet()
            val resolved = candidates.firstOrNull { it in available }
                ?: candidates.firstOrNull { name ->
                    Font(name, Font.PLAIN, 16).family.equals(name, ignoreCase = true)
                }

            val font = if (resolved != null) Font(resolved, Font.PLAIN, sizePt.toInt())
                       else Font(Font.SANS_SERIF, Font.PLAIN, sizePt.toInt())
            cachedEmojiFont = font
            return font.deriveFont(sizePt)
        }
    }
}
