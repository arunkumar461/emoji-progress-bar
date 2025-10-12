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
import javax.swing.UIDefaults
import javax.swing.UIManager
import javax.swing.plaf.basic.BasicProgressBarUI
import javax.swing.plaf.ComponentUI
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
        val trackColor = fetchColor("ProgressBar.trackColor", UIUtil.getPanelBackground())
        val progressColor = fetchColor("ProgressBar.progressColor", UIUtil.getLabelForeground())

        val shape = RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), radius.toFloat(), radius.toFloat())
        g2.color = trackColor
        g2.fill(shape)

        val fraction = (computeFractionOverride ?: bar.percentComplete ?: 0.0).coerceIn(0.0, 1.0)
        if (fraction > 0.0) {
            g2.color = progressColor
            val progressWidth = max(1, (width * fraction).toInt())
            val progressShape =
                RoundRectangle2D.Float(0f, 0f, progressWidth.toFloat(), height.toFloat(), radius.toFloat(), radius.toFloat())
            g2.fill(progressShape)
        }

        paintEmojiTrack(g2, width, height, fraction, state)

        g2.dispose()
    }

    private fun paintEmojiTrack(
        g2: Graphics2D,
        width: Int,
        height: Int,
        fraction: Double,
        state: EmojiProgressBarSettings.State
    ) {
        val emojiTokens = parseEmojiTokens(state.emojiSequence).ifEmpty {
            listOf(EmojiProgressBarSettings.DEFAULT_EMOJI_SEQUENCE)
        }
        val filler = state.trackCharacter.takeUnless { it.isBlank() } ?: EmojiProgressBarSettings.DEFAULT_TRACK_CHARACTER

        val fm = g2.fontMetrics
        val spaceWidth = fm.charWidth(' ').takeIf { it > 0 } ?: JBUI.scale(2)
        val slotWidthGuess = (emojiTokens + filler).map { tokenWidth(it, fm) }.maxOrNull() ?: fm.height
        val slotWidth = slotWidthGuess + spaceWidth
        val totalSlots = max(1, minOf(MAX_EMOJI_SLOTS, (width.toDouble() / slotWidth).roundToInt().coerceAtLeast(1)))

        val filledSlots = (totalSlots * fraction).roundToInt().coerceIn(0, totalSlots)

        val tokens = MutableList(totalSlots) { filler }
        for (index in 0 until filledSlots) {
            tokens[index] = emojiTokens[index % emojiTokens.size]
        }

        val tokenWidths = tokens.map { tokenWidth(it, fm) }
        val totalWidth = tokenWidths.sum() + spaceWidth * (totalSlots - 1)
        val startX = ((width - totalWidth) / 2).coerceAtLeast(0)
        val baseline = ((height - fm.height) / 2) + fm.ascent

        var cursorX = startX
        for (index in tokens.indices) {
            val token = tokens[index]
            val color = if (index < filledSlots) fetchColor("ProgressBar.foreground", UIUtil.getLabelForeground())
            else UIUtil.getLabelDisabledForeground()
            g2.color = color
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

    private fun fetchColor(key: String, fallback: Color): Color =
        UIManager.getColor(key) ?: JBColor(fallback, fallback)

    companion object {
        private const val MAX_EMOJI_SLOTS = 12
        private const val INDETERMINATE_STEPS = 24L
        private const val MIN_ANIMATION_MS = 45
        private const val MAX_ANIMATION_MS = 600

        val UI_CLASS_NAME: String = EmojiProgressBarUi::class.java.name

        fun asActiveValue(): UIDefaults.ActiveValue = UIDefaults.ActiveValue {
            EmojiProgressBarUi()
        }
    }
}
