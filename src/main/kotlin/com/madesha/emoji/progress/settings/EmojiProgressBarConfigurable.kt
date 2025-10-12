package com.madesha.emoji.progress.settings

import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class EmojiProgressBarConfigurable : SearchableConfigurable {

    private val settings: EmojiProgressBarSettings = EmojiProgressBarSettings.getInstance()
    private var component: EmojiProgressBarSettingsComponent? = null

    override fun getId(): String = "preferences.EmojiProgressBar"

    override fun getDisplayName(): String = "Emoji Progress Bar"

    override fun createComponent(): JComponent {
        if (component == null) {
            component = EmojiProgressBarSettingsComponent().also { ui ->
                val state = settings.state
                ui.emojiSequence = state.emojiSequence
                ui.trackCharacter = state.trackCharacter
                ui.indeterminateSpeedMs = state.indeterminateSpeedMs
            }
        }
        return component!!.panel
    }

    override fun isModified(): Boolean {
        val ui = component ?: return false
        val state = settings.state
        return state.emojiSequence != ui.emojiSequence ||
            state.trackCharacter != ui.trackCharacter ||
            state.indeterminateSpeedMs != ui.indeterminateSpeedMs
    }

    override fun apply() {
        val ui = component ?: return
        settings.update {
            it.emojiSequence = ui.emojiSequence
            it.trackCharacter = ui.trackCharacter
            it.indeterminateSpeedMs = ui.indeterminateSpeedMs
        }
    }

    override fun reset() {
        val ui = component ?: return
        val state = settings.state
        ui.emojiSequence = state.emojiSequence
        ui.trackCharacter = state.trackCharacter
        ui.indeterminateSpeedMs = state.indeterminateSpeedMs
    }

    override fun getPreferredFocusedComponent(): JComponent? =
        component?.preferredFocusedComponent()

    override fun disposeUIResources() {
        component = null
    }
}
