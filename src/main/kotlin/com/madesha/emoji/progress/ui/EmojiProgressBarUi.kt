package com.madesha.emoji.progress.ui

import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.madesha.emoji.progress.settings.EmojiProgressBarSettings
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.UIManager
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
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val insets = bar.insets
        val width = bar.width - insets.left - insets.right
        val height = bar.height - insets.top - insets.bottom
        if (width <= 0 || height <= 0) {
            g2.dispose()
            return
        }

        g2.translate(insets.left, insets.top)

        val radius = max(8, ceil(height * 0.8).toInt())
        val trackColor = fetchColor(
            key = "ProgressBar.trackColor",
            lightFallback = Color(0xE6EAF5),
            darkFallback = Color(0x2A2B32)
        )
        val progressColor = fetchColor(
            key = "ProgressBar.progressColor",
            lightFallback = Color(0xFF7A7A),
            darkFallback = Color(0xFFAF5F)
        )
        val progressAccent = fetchColor(
            key = "ProgressBar.progressAccentColor",
            lightFallback = Color(0xFFB56B),
            darkFallback = Color(0xFFDD7F)
        )

        val trackPaint = LinearGradientPaint(
            0f,
            0f,
            width.toFloat(),
            0f,
            floatArrayOf(0f, 1f),
            arrayOf(ColorUtil.brighter(trackColor, 1.06.toInt()), ColorUtil.darker(trackColor, 1.05.toInt()))
        )
        val shape = RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), radius.toFloat(), radius.toFloat())
        g2.paint = trackPaint
        g2.fill(shape)

        val fraction = (computeFractionOverride ?: bar.percentComplete ?: 0.0).coerceIn(0.0, 1.0)
        if (fraction > 0.0) {
            val progressWidth = max(1, (width * fraction).toInt())
            val progressShape =
                RoundRectangle2D.Float(0f, 0f, progressWidth.toFloat(), height.toFloat(), radius.toFloat(), radius.toFloat())
            val progressPaint = LinearGradientPaint(
                0f,
                0f,
                progressWidth.toFloat(),
                0f,
                floatArrayOf(0f, 0.5f, 1f),
                arrayOf(
                    ColorUtil.brighter(progressColor, 1.15.toInt()),
                    progressAccent,
                    ColorUtil.darker(progressColor, 1.05.toInt())
                )
            )
            g2.paint = progressPaint
            g2.fill(progressShape)
        }

        paintEmojiTrack(g2, width, height, fraction, state, System.currentTimeMillis())

        g2.dispose()
    }

    private fun paintEmojiTrack(
        g2: Graphics2D,
        width: Int,
        height: Int,
        fraction: Double,
        state: EmojiProgressBarSettings.State,
        timestamp: Long
    ) {
        val emojiTokens = parseEmojiTokens(state.emojiSequence).ifEmpty {
            listOf(EmojiProgressBarSettings.DEFAULT_EMOJI_SEQUENCE)
        }
        val filler = state.trackCharacter.takeUnless { it.isBlank() } ?: EmojiProgressBarSettings.DEFAULT_TRACK_CHARACTER
        val animationStepMs = state.indeterminateSpeedMs.coerceIn(60, 400).toLong()
        val emojiPhase = if (emojiTokens.size == 1) 0 else ((timestamp / animationStepMs) % emojiTokens.size).toInt()

        val fm = g2.fontMetrics
        val spaceWidth = fm.charWidth(' ').takeIf { it > 0 } ?: JBUI.scale(2)
        val slotWidthGuess = (emojiTokens + filler).map { tokenWidth(it, fm) }.maxOrNull() ?: fm.height
        val slotWidth = slotWidthGuess + spaceWidth
        val totalSlots = max(1, minOf(MAX_EMOJI_SLOTS, (width.toDouble() / slotWidth).roundToInt().coerceAtLeast(1)))

        val filledSlots = (totalSlots * fraction).roundToInt().coerceIn(0, totalSlots)

        val tokens = MutableList(totalSlots) { filler }
        for (index in 0 until filledSlots) {
            val shiftedIndex = (index + emojiPhase) % emojiTokens.size
            tokens[index] = emojiTokens[shiftedIndex]
        }

        val tokenWidths = tokens.map { tokenWidth(it, fm) }
        val totalWidth = tokenWidths.sum() + spaceWidth * (totalSlots - 1)
        val startX = ((width - totalWidth) / 2).coerceAtLeast(0)
        val baseline = ((height - fm.height) / 2) + fm.ascent

        var cursorX = startX
        for (index in tokens.indices) {
            val token = tokens[index]
            if (index < filledSlots) {
                val gradient = LinearGradientPaint(
                    cursorX.toFloat(),
                    0f,
                    (cursorX + tokenWidths[index]).toFloat(),
                    0f,
                    floatArrayOf(0f, 1f),
                    arrayOf(
                        fetchColor(
                            key = "ProgressBar.foreground",
                            lightFallback = Color(0x2E1F5E),
                            darkFallback = Color(0xFFE6FF)
                        ),
                        fetchColor(
                            key = "ProgressBar.foregroundSecondary",
                            lightFallback = Color(0xFF8A65),
                            darkFallback = Color(0xFFDD95)
                        )
                    )
                )
                g2.paint = gradient
            } else {
                g2.color = UIUtil.getLabelDisabledForeground()
            }
            g2.drawString(token, cursorX, baseline)
            cursorX += tokenWidths[index]
            if (index < tokens.lastIndex) {
                cursorX += spaceWidth
            }
        }
    }

    private fun tokenWidth(token: String, fm: java.awt.FontMetrics): Int {
        val width = fm.stringWidth(token)
        return if (width > 0) width else max(fm.height / 2, JBUI.scale(8))
    }

    private fun parseEmojiTokens(raw: String): List<String> =
        raw.split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun fetchColor(key: String, lightFallback: Color, darkFallback: Color): Color =
        UIManager.getColor(key) ?: JBColor(lightFallback, darkFallback)

    companion object {
        private const val MAX_EMOJI_SLOTS = 12
        private const val INDETERMINATE_STEPS = 24L
        private const val MIN_ANIMATION_MS = 45
        private const val MAX_ANIMATION_MS = 600

        val UI_CLASS_NAME: String = EmojiProgressBarUi::class.java.name

        @JvmStatic
        fun createUI(component: JComponent?): BasicProgressBarUI = EmojiProgressBarUi()
    }
}
