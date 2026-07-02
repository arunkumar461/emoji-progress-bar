package com.madesha.emoji.progress.settings

import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.roundToInt

class EmojiProgressBarSettingsComponent {

    private val emojiField = JBTextField()
    private val trackField = JBTextField()
    private val speedSlider = createSpeedSlider()
    private val trackColorPanel = ColorPanel()
    private val progressColorPanel = ColorPanel()
    private val borderColorPanel = ColorPanel()
    private val indicatorSizeSlider = createIndicatorSizeSlider()
    private val previewLabel = JBLabel("", SwingConstants.CENTER)
    private val previewWrapper = JBPanelWithEmptyText(BorderLayout())
    private val basePreviewFont = previewLabel.font

    init {
        previewLabel.border = JBUI.Borders.empty(8)
        previewLabel.font = previewLabel.font.deriveFont(emojiLabelBaseFontSize())
        previewLabel.isOpaque = true
        previewWrapper.isOpaque = true
        previewWrapper.add(previewLabel, BorderLayout.CENTER)
        previewWrapper.emptyText.text = "Your emoji progress bar preview appears here"
        previewWrapper.preferredSize = java.awt.Dimension(JBUI.scale(260), JBUI.scale(72))

        val changeListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updatePreview()
            override fun removeUpdate(e: DocumentEvent?) = updatePreview()
            override fun changedUpdate(e: DocumentEvent?) = updatePreview()
        }
        emojiField.document.addDocumentListener(changeListener)
        trackField.document.addDocumentListener(changeListener)
        speedSlider.addChangeListener { updatePreview() }
        listOf(trackColorPanel, progressColorPanel, borderColorPanel).forEach { it.addActionListener { updatePreview() } }
        indicatorSizeSlider.addChangeListener { updatePreview() }

        resetToDefaults()
    }

    private val resetButton = JButton("Reset to Defaults").apply {
        addActionListener { resetToDefaults() }
    }

    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Emoji sequence:"), emojiField, 1, false)
        .addLabeledComponent(JBLabel("Track filler character:"), trackField, 1, false)
        .addLabeledComponent(JBLabel("Animation speed (ms):"), speedSlider, 1, false)
        .addLabeledComponent(JBLabel("Track color:"), trackColorPanel, 1, false)
        .addLabeledComponent(JBLabel("Progress color:"), progressColorPanel, 1, false)
        .addLabeledComponent(JBLabel("Border color:"), borderColorPanel, 1, false)
        .addLabeledComponent(JBLabel("Indicator size (%):"), indicatorSizeSlider, 1, false)
        .addComponent(previewWrapper, 1)
        .addComponent(resetButton, 1)
        .panel

    var emojiSequence: String
        get() = emojiField.text
        set(value) { emojiField.text = value; updatePreview() }

    var trackCharacter: String
        get() = trackField.text
        set(value) { trackField.text = value; updatePreview() }

    var indeterminateSpeedMs: Int
        get() = speedSlider.value
        set(value) { speedSlider.value = value; updatePreview() }

    var trackColorHex: String
        get() = colorToHex(trackColorPanel, EmojiProgressBarSettings.DEFAULT_TRACK_COLOR)
        set(value) { trackColorPanel.selectedColor = parseColor(value, EmojiProgressBarSettings.DEFAULT_TRACK_COLOR); updatePreview() }

    var progressColorHex: String
        get() = colorToHex(progressColorPanel, EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR)
        set(value) { progressColorPanel.selectedColor = parseColor(value, EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR); updatePreview() }

    var borderColorHex: String
        get() = colorToHex(borderColorPanel, EmojiProgressBarSettings.DEFAULT_BORDER_COLOR)
        set(value) { borderColorPanel.selectedColor = parseColor(value, EmojiProgressBarSettings.DEFAULT_BORDER_COLOR); updatePreview() }

    var indicatorScalePercent: Int
        get() = indicatorSizeSlider.value
        set(value) { indicatorSizeSlider.value = value; updatePreview() }

    fun preferredFocusedComponent(): JComponent = emojiField

    fun resetToDefaults() {
        emojiField.text = EmojiProgressBarSettings.DEFAULT_EMOJI_SEQUENCE
        trackField.text = EmojiProgressBarSettings.DEFAULT_TRACK_CHARACTER
        speedSlider.value = EmojiProgressBarSettings.DEFAULT_SPEED_MS
        trackColorPanel.selectedColor = parseColor(EmojiProgressBarSettings.DEFAULT_TRACK_COLOR)
        progressColorPanel.selectedColor = parseColor(EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR)
        borderColorPanel.selectedColor = parseColor(EmojiProgressBarSettings.DEFAULT_BORDER_COLOR)
        indicatorSizeSlider.value = EmojiProgressBarSettings.DEFAULT_INDICATOR_SCALE_PERCENT
        updatePreview()
    }

    private fun updatePreview() {
        val emojiTokens = emojiSequence.takeUnless { it.isBlank() }
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.ifEmpty { null }
            ?: listOf(EmojiProgressBarSettings.DEFAULT_EMOJI_SEQUENCE)

        val filler = trackCharacter.takeUnless { it.isBlank() }
            ?: EmojiProgressBarSettings.DEFAULT_TRACK_CHARACTER

        val indicatorScale = indicatorSizeSlider.value / 100.0
        val filledSlots = 6
        val indicator = buildString {
            for (index in 0 until filledSlots) {
                val token = emojiTokens[index % emojiTokens.size]
                append(if (index <= (filledSlots * 0.6).roundToInt()) token else filler)
                if (token.length == 1 && !Character.isSurrogate(token[0])) append(' ')
            }
        }

        previewWrapper.background = trackColorPanel.selectedColor
        previewLabel.isOpaque = true
        previewLabel.background = trackColorPanel.selectedColor
        previewLabel.font = basePreviewFont.deriveFont((basePreviewFont.size2D * indicatorScale).toFloat().coerceAtMost(48f))
        previewLabel.icon = null
        previewLabel.text = indicator
        previewLabel.foreground = UIUtil.getLabelForeground()

        val borderColor = borderColorPanel.selectedColor ?: parseColor(EmojiProgressBarSettings.DEFAULT_BORDER_COLOR)
        previewLabel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(borderColor, 1),
            JBUI.Borders.empty(6)
        )
        previewWrapper.emptyText.clear()
    }

    private fun emojiLabelBaseFontSize(): Float =
        (JBUI.Fonts.label().size.toFloat() * 1.6f).coerceAtMost(32f)

    private fun createSpeedSlider(): JSlider =
        JSlider(SwingConstants.HORIZONTAL, EmojiProgressBarSettings.MIN_SPEED_MS, EmojiProgressBarSettings.MAX_SPEED_MS, EmojiProgressBarSettings.DEFAULT_SPEED_MS).apply {
            majorTickSpacing = 80
            minorTickSpacing = 20
            paintTicks = true
            paintLabels = true
        }

    private fun createIndicatorSizeSlider(): JSlider =
        JSlider(
            SwingConstants.HORIZONTAL,
            EmojiProgressBarSettings.MIN_INDICATOR_SCALE_PERCENT,
            EmojiProgressBarSettings.MAX_INDICATOR_SCALE_PERCENT,
            EmojiProgressBarSettings.DEFAULT_INDICATOR_SCALE_PERCENT
        ).apply {
            majorTickSpacing = 50
            minorTickSpacing = 10
            paintTicks = true
            paintLabels = true
        }

    private fun colorToHex(panel: ColorPanel, fallback: String): String {
        val color = panel.selectedColor ?: parseColor(fallback, fallback)
        return String.format("%06X", color.rgb and 0xFFFFFF)
    }

    private fun parseColor(hex: String, fallbackHex: String = EmojiProgressBarSettings.DEFAULT_TRACK_COLOR): Color {
        return decodeColor(hex) ?: decodeColor(fallbackHex) ?: Color(0xFF, 0xF3, 0xCD)
    }

    private fun decodeColor(value: String?): Color? {
        val cleaned = value?.trim()?.removePrefix("#") ?: return null
        return try {
            val v = cleaned.toInt(16)
            Color(v shr 16 and 0xFF, v shr 8 and 0xFF, v and 0xFF)
        } catch (_: Exception) {
            null
        }
    }
}
