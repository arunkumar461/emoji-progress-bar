package com.madesha.emoji.progress.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
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
    private val previewLabel = JBLabel("", SwingConstants.CENTER)
    private val previewWrapper = JBPanelWithEmptyText(BorderLayout())

    init {
        previewLabel.border = JBUI.Borders.empty(8)
        previewLabel.font = previewLabel.font.deriveFont(emojiLabelBaseFontSize())
        previewWrapper.add(previewLabel, BorderLayout.CENTER)
        previewWrapper.emptyText.text = "Your emoji progress bar preview appears here"

        val changeListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updatePreview()
            override fun removeUpdate(e: DocumentEvent?) = updatePreview()
            override fun changedUpdate(e: DocumentEvent?) = updatePreview()
        }
        emojiField.document.addDocumentListener(changeListener)
        trackField.document.addDocumentListener(changeListener)

        speedSlider.addChangeListener { updatePreview() }

        updatePreview()
    }

    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Emoji sequence:"), emojiField, 1, false)
        .addLabeledComponent(JBLabel("Track filler character:"), trackField, 1, false)
        .addLabeledComponent(JBLabel("Indeterminate animation speed (ms):"), speedSlider, 1, false)
        .addComponent(previewWrapper, 1)
        .panel

    var emojiSequence: String
        get() = emojiField.text
        set(value) {
            emojiField.text = value
            updatePreview()
        }

    var trackCharacter: String
        get() = trackField.text
        set(value) {
            trackField.text = value
            updatePreview()
        }

    var indeterminateSpeedMs: Int
        get() = speedSlider.value
        set(value) {
            speedSlider.value = value
            updatePreview()
        }

    fun preferredFocusedComponent(): JComponent = emojiField

    private fun updatePreview() {
        val emojiTokens = emojiSequence.takeUnless { it.isBlank() }
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.ifEmpty { null }
        val emoji = emojiTokens ?: listOf(EmojiProgressBarSettings.DEFAULT_EMOJI_SEQUENCE)

        val filler = trackCharacter.takeUnless { it.isBlank() }
            ?: EmojiProgressBarSettings.DEFAULT_TRACK_CHARACTER

        val filledSlots = 6
        val indicator = buildString {
            for (index in 0 until filledSlots) {
                val emojiToken = emoji[index % emoji.size]
                append(if (index <= (filledSlots * 0.6).roundToInt()) emojiToken else filler)
                if (emojiToken.length == 1 && !Character.isSurrogate(emojiToken[0])) {
                    append(' ')
                }
            }
        }
        previewLabel.text = indicator
        previewWrapper.emptyText.clear()
    }

    private fun emojiLabelBaseFontSize(): Float {
        val base = JBUI.Fonts.label().size.toFloat()
        return (base * 1.6f).coerceAtMost(32f)
    }

    private fun createSpeedSlider(): JSlider =
        JSlider(
            SwingConstants.HORIZONTAL,
            60,
            400,
            EmojiProgressBarSettings.State().indeterminateSpeedMs
        ).apply {
            majorTickSpacing = 80
            minorTickSpacing = 20
            paintTicks = true
            paintLabels = true
        }
}
