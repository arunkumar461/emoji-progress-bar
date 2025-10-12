package com.madesha.emoji.progress.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.madesha.emoji.progress.settings.EmojiProgressBarSettings
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.UIManager
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.ceil
import kotlin.math.max

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
        val trackBackground = fetchColor(
            key = "ProgressBar.trackColor",
            lightFallback = Color(0xF2F4F9),
            darkFallback = Color(0x2B2D32)
        )
        val progressFill = fetchColor(
            key = "ProgressBar.progressColor",
            lightFallback = Color.WHITE,
            darkFallback = Color(0x3B4048)
        )
        val borderColor = fetchColor(
            key = "ProgressBar.borderColor",
            lightFallback = Color(0xD0D4E0),
            darkFallback = Color(0x43464E)
        )

        val shape =
            RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), radius.toFloat(), radius.toFloat())
        g2.color = trackBackground
        g2.fill(shape)

        val fraction = (computeFractionOverride ?: bar.percentComplete ?: 0.0).coerceIn(0.0, 1.0)
        val progressWidth = max(1, (width * fraction).toInt())
        if (fraction > 0.0) {
            val progressShape =
                RoundRectangle2D.Float(
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

        paintEmojiIndicator(
            g2 = g2,
            width = width,
            height = height,
            fraction = fraction,
            state = state,
            timestamp = System.currentTimeMillis(),
            progressWidth = progressWidth
        )

        g2.dispose()
    }

    private fun paintEmojiIndicator(
        g2: Graphics2D,
        width: Int,
        height: Int,
        fraction: Double,
        state: EmojiProgressBarSettings.State,
        timestamp: Long,
        progressWidth: Int
    ) {
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

        val fm = g2.fontMetrics
        val emojiWidthRaw = fm.stringWidth(emoji)
        val emojiWidth = emojiWidthRaw.coerceAtLeast(JBUI.scale(12))
        val availableWidth = (width - emojiWidth).coerceAtLeast(0)
        val baseline = ((height - fm.height) / 2) + fm.ascent

        val emojiX = (progressWidth - emojiWidth).coerceIn(0, availableWidth)

        g2.color = UIUtil.getLabelForeground()
        g2.drawString(emoji, emojiX, baseline)
    }

    private fun parseEmojiTokens(raw: String): List<String> =
        raw.split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun fetchColor(key: String, lightFallback: Color, darkFallback: Color): Color =
        UIManager.getColor(key) ?: JBColor(lightFallback, darkFallback)

    companion object {
        private const val INDETERMINATE_STEPS = 24L
        private const val MIN_ANIMATION_MS = 45
        private const val MAX_ANIMATION_MS = 600

        val UI_CLASS_NAME: String = EmojiProgressBarUi::class.java.name

        @JvmStatic
        fun createUI(component: JComponent?): BasicProgressBarUI = EmojiProgressBarUi()
    }
}
