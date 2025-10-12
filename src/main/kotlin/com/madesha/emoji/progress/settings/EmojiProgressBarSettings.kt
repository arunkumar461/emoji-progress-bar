package com.madesha.emoji.progress.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic

@State(name = "EmojiProgressBarSettings", storages = [Storage("emojiProgressBarSettings.xml")])
@Service(Service.Level.APP)
class EmojiProgressBarSettings : PersistentStateComponent<EmojiProgressBarSettings.State> {

    data class State(
        var emojiSequence: String = "\uD83D\uDC31\u200D\uD83D\uDC64 \uD83C\uDF0C",
        var trackCharacter: String = "\u00B7",
        var indeterminateSpeedMs: Int = 120,
        var trackColorHex: String = DEFAULT_TRACK_COLOR,
        var progressColorHex: String = DEFAULT_PROGRESS_COLOR,
        var borderColorHex: String = DEFAULT_BORDER_COLOR,
        var useImageIndicator: Boolean = false,
        var imagePath: String = ""
    )

    private var state: State = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state.normalize()
    }

    fun update(mutate: (State) -> Unit) {
        mutate(state)
        state = state.normalize()
        notifyListeners()
    }

    private fun notifyListeners() {
        val publisher = ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC)
        publisher.settingsChanged(state)
    }

    private fun State.normalize(): State = State(
        emojiSequence = emojiSequence.takeUnless { it.isBlank() } ?: DEFAULT_EMOJI_SEQUENCE,
        trackCharacter = trackCharacter.takeUnless { it.isBlank() } ?: DEFAULT_TRACK_CHARACTER,
        indeterminateSpeedMs = indeterminateSpeedMs.coerceIn(MIN_SPEED_MS, MAX_SPEED_MS),
        trackColorHex = normalizeColor(trackColorHex, DEFAULT_TRACK_COLOR),
        progressColorHex = normalizeColor(progressColorHex, DEFAULT_PROGRESS_COLOR),
        borderColorHex = normalizeColor(borderColorHex, DEFAULT_BORDER_COLOR),
        useImageIndicator = useImageIndicator,
        imagePath = imagePath.trim()
    )

    companion object {
        const val DEFAULT_EMOJI_SEQUENCE: String = "\uD83D\uDE38\u200D\u2B1B"
        const val DEFAULT_TRACK_CHARACTER: String = "\u00B7"
        const val DEFAULT_TRACK_COLOR: String = "F2F4F9"
        const val DEFAULT_PROGRESS_COLOR: String = "FFFFFF"
        const val DEFAULT_BORDER_COLOR: String = "D0D4E0"
        private const val MIN_SPEED_MS: Int = 60
        private const val MAX_SPEED_MS: Int = 400

        val TOPIC: Topic<EmojiProgressBarSettingsListener> =
            Topic.create("EmojiProgressBarSettingsChanged", EmojiProgressBarSettingsListener::class.java)

        fun getInstance(): EmojiProgressBarSettings =
            ApplicationManager.getApplication().getService(EmojiProgressBarSettings::class.java)

        private fun normalizeColor(value: String?, fallback: String): String {
            val cleaned = value?.trim()?.removePrefix("#") ?: return fallback
            return cleaned.takeIf { it.matches(Regex("[0-9a-fA-F]{6,8}")) }?.uppercase() ?: fallback
        }
    }

    interface EmojiProgressBarSettingsListener {
        fun settingsChanged(state: State)
    }
}
