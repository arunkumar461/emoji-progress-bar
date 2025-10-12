package com.madesha.emoji.progress.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.ui.ColorPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
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
    private val useImageCheckbox = JBCheckBox("Use custom image indicator")
    private val imageField = TextFieldWithBrowseButton()
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

        val changeListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updatePreview()
            override fun removeUpdate(e: DocumentEvent?) = updatePreview()
            override fun changedUpdate(e: DocumentEvent?) = updatePreview()
        }
        emojiField.document.addDocumentListener(changeListener)
        trackField.document.addDocumentListener(changeListener)

        speedSlider.addChangeListener { updatePreview() }

        listOf(trackColorPanel, progressColorPanel, borderColorPanel).forEach { panel ->
            panel.addActionListener { updatePreview() }
        }

        useImageCheckbox.addActionListener {
            imageField.isEnabled = useImageCheckbox.isSelected
            updatePreview()
        }

        indicatorSizeSlider.addChangeListener { updatePreview() }

        imageField.addBrowseFolderListener(
            "Select Indicator Image",
            "Choose an image to display inside the progress bar",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
        imageField.textField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updatePreview()
            override fun removeUpdate(e: DocumentEvent?) = updatePreview()
            override fun changedUpdate(e: DocumentEvent?) = updatePreview()
        })

        trackColorPanel.setSelectedColor(parseColor(EmojiProgressBarSettings.DEFAULT_TRACK_COLOR))
        progressColorPanel.setSelectedColor(
            parseColor(
                EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR,
                EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR
            )
        )
        borderColorPanel.setSelectedColor(
            parseColor(
                EmojiProgressBarSettings.DEFAULT_BORDER_COLOR,
                EmojiProgressBarSettings.DEFAULT_BORDER_COLOR
            )
        )
        imageField.isEnabled = false

        updatePreview()
    }

    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Emoji sequence:"), emojiField, 1, false)
        .addLabeledComponent(JBLabel("Track filler character:"), trackField, 1, false)
        .addLabeledComponent(JBLabel("Indeterminate animation speed (ms):"), speedSlider, 1, false)
        .addLabeledComponent(JBLabel("Track color:"), trackColorPanel, 1, false)
        .addLabeledComponent(JBLabel("Progress color:"), progressColorPanel, 1, false)
        .addLabeledComponent(JBLabel("Border color:"), borderColorPanel, 1, false)
        .addLabeledComponent(JBLabel("Indicator size (%):"), indicatorSizeSlider, 1, false)
        .addComponent(useImageCheckbox, 1)
        .addLabeledComponent(JBLabel("Image file:"), imageField, 1, false)
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

    var trackColorHex: String
        get() = colorToHex(trackColorPanel, EmojiProgressBarSettings.DEFAULT_TRACK_COLOR)
        set(value) {
            trackColorPanel.setSelectedColor(parseColor(value, EmojiProgressBarSettings.DEFAULT_TRACK_COLOR))
            updatePreview()
        }

    var progressColorHex: String
        get() = colorToHex(progressColorPanel, EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR)
        set(value) {
            progressColorPanel.setSelectedColor(parseColor(value, EmojiProgressBarSettings.DEFAULT_PROGRESS_COLOR))
            updatePreview()
        }

    var borderColorHex: String
        get() = colorToHex(borderColorPanel, EmojiProgressBarSettings.DEFAULT_BORDER_COLOR)
        set(value) {
            borderColorPanel.setSelectedColor(parseColor(value, EmojiProgressBarSettings.DEFAULT_BORDER_COLOR))
            updatePreview()
        }

    var indicatorScalePercent: Int
        get() = indicatorSizeSlider.value
        set(value) {
            indicatorSizeSlider.value = value
            updatePreview()
        }

    var useImageIndicator: Boolean
        get() = useImageCheckbox.isSelected
        set(value) {
            useImageCheckbox.isSelected = value
            imageField.isEnabled = value
            updatePreview()
        }

    var imagePath: String
        get() = imageField.text.trim()
        set(value) {
            imageField.text = value
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

        val indicatorScale = indicatorSizeSlider.value / 100.0

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

        previewWrapper.background = trackColorPanel.selectedColor
        previewLabel.isOpaque = true
        previewLabel.background = trackColorPanel.selectedColor

        previewLabel.font = basePreviewFont.deriveFont((basePreviewFont.size2D * indicatorScale).toFloat().coerceAtMost(48f))

        if (useImageCheckbox.isSelected) {
            val icon = loadPreviewIcon(imagePath, indicatorScale)
            if (icon != null) {
                previewLabel.icon = icon
                previewLabel.text = ""
            } else {
                previewLabel.icon = null
                previewLabel.text = indicator
            }
        } else {
            previewLabel.icon = null
            previewLabel.text = indicator
        }
        previewLabel.foreground = UIUtil.getLabelForeground()
        val borderColor = borderColorPanel.selectedColor ?: parseColor(EmojiProgressBarSettings.DEFAULT_BORDER_COLOR)
        previewLabel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(borderColor, 1),
            JBUI.Borders.empty(6)
        )
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

    private fun createIndicatorSizeSlider(): JSlider =
        JSlider(
            SwingConstants.HORIZONTAL,
            50,
            300,
            EmojiProgressBarSettings.DEFAULT_INDICATOR_SCALE_PERCENT
        ).apply {
            majorTickSpacing = 50
            minorTickSpacing = 10
            paintTicks = true
            paintLabels = true
        }

    private fun colorToHex(panel: ColorPanel, fallback: String): String {
        val color = panel.selectedColor ?: parseColor(fallback, fallback)
        val rgb = color.rgb and 0xFFFFFF
        return String.format("%06X", rgb)
    }

    private fun parseColor(hex: String, fallbackHex: String = EmojiProgressBarSettings.DEFAULT_TRACK_COLOR): Color {
        return decodeColor(hex) ?: decodeColor(fallbackHex) ?: DEFAULT_PREVIEW_TRACK_COLOR
    }

    private fun decodeColor(value: String?): Color? {
        val cleaned = value?.trim()?.removePrefix("#") ?: return null
        return try {
            val intValue = cleaned.toInt(16)
            Color(intValue shr 16 and 0xFF, intValue shr 8 and 0xFF, intValue and 0xFF)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadPreviewIcon(path: String, scale: Double): ImageIcon? {
        val file = path.takeIf { it.isNotBlank() }?.let { File(it) } ?: return null
        if (!file.exists()) return null
        return try {
            val image: BufferedImage = ImageIO.read(file) ?: return null
            val baseHeight = JBUI.scale(28)
            val targetHeight = (baseHeight * scale).roundToInt().coerceAtLeast(JBUI.scale(16))
            val scale = targetHeight.toDouble() / image.height.coerceAtLeast(1)
            val targetWidth = (image.width * scale).roundToInt().coerceAtLeast(JBUI.scale(16))
            val scaled = image.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH)
            ImageIcon(scaled)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val DEFAULT_PREVIEW_TRACK_COLOR = Color(0xF2, 0xF4, 0xF9)
    }
}
