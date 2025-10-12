package com.madesha.emoji.progress.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.madesha.emoji.progress.settings.EmojiProgressBarSettings
import java.awt.Component
import java.awt.Container
import java.awt.EventQueue
import java.awt.Frame
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JProgressBar
import javax.swing.UIDefaults
import javax.swing.UIManager

@Service(Service.Level.APP)
class EmojiProgressBarManager : Disposable {

    private val uiKey: String = EmojiProgressBarUi.UI_CLASS_NAME
    private val application get() = ApplicationManager.getApplication()

    private var originalCaptured: Boolean = false
    private var originalProgressBarUi: Any? = null
    private var originalNamedUi: Any? = null
    private var originalLafUi: Any? = null
    private var originalLafNamedUi: Any? = null
    private val initialized = AtomicBoolean(false)

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        if (application.isDisposed) return

        captureOriginalDefaults()
        installCustomProgressBarUi()
        connectSettingUpdates()
    }

    private fun captureOriginalDefaults() {
        if (originalCaptured) return
        val defaults = UIManager.getDefaults()
        val lafDefaults = UIManager.getLookAndFeelDefaults()

        originalProgressBarUi = defaults["ProgressBarUI"]
        originalNamedUi = defaults[uiKey]
        originalLafUi = lafDefaults["ProgressBarUI"]
        originalLafNamedUi = lafDefaults[uiKey]
        originalCaptured = true
    }

    private fun installCustomProgressBarUi() {
        val defaults = UIManager.getDefaults()
        val lafDefaults = UIManager.getLookAndFeelDefaults()
        val uiClassName = uiKey
        val uiClass = EmojiProgressBarUi::class.java

        defaults["ProgressBarUI"] = uiClassName
        defaults[uiClassName] = uiClass
        lafDefaults["ProgressBarUI"] = uiClassName
        lafDefaults[uiClassName] = uiClass

        forceRefreshOfOpenProgressBars()
    }

    private fun connectSettingUpdates() {
        val connection = application.messageBus.connect(this)
        connection.subscribe(
            EmojiProgressBarSettings.TOPIC,
            object : EmojiProgressBarSettings.EmojiProgressBarSettingsListener {
                override fun settingsChanged(state: EmojiProgressBarSettings.State) {
                    forceRefreshOfOpenProgressBars()
                }
            }
        )
        connection.subscribe(
            LafManagerListener.TOPIC,
            LafManagerListener {
                installCustomProgressBarUi()
            }
        )
    }

    private fun forceRefreshOfOpenProgressBars() {
        if (application.isDisposed) return
        EventQueue.invokeLater {
            Frame.getFrames().forEach { frame ->
                if (frame.isDisplayable) {
                    updateProgressBars(frame)
                }
            }
        }
    }

    private fun updateProgressBars(component: Component) {
        if (component is JProgressBar) {
            component.updateUI()
            component.revalidate()
            component.repaint()
        }
        if (component is Container) {
            component.components.forEach { child ->
                updateProgressBars(child)
            }
        }
    }

    override fun dispose() {
        val defaults = UIManager.getDefaults()
        val lafDefaults = UIManager.getLookAndFeelDefaults()
        restoreOriginal(defaults, "ProgressBarUI", originalProgressBarUi)
        restoreOriginal(defaults, uiKey, originalNamedUi)
        restoreOriginal(lafDefaults, "ProgressBarUI", originalLafUi)
        restoreOriginal(lafDefaults, uiKey, originalLafNamedUi)
        forceRefreshOfOpenProgressBars()
    }

    private fun restoreOriginal(table: UIDefaults, key: String, value: Any?) {
        if (value != null) {
            table[key] = value
        } else {
            table.remove(key)
        }
    }
}
