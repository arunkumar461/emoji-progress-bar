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
import javax.swing.JProgressBar
import javax.swing.UIDefaults
import javax.swing.UIManager

@Service(Service.Level.APP)
class EmojiProgressBarManager : Disposable {

    private val defaults: UIDefaults = UIManager.getDefaults()
    private val lafDefaults: UIDefaults = UIManager.getLookAndFeelDefaults()
    private val uiKey: String = EmojiProgressBarUi.UI_CLASS_NAME
    private val originalProgressBarUi: Any? = defaults["ProgressBarUI"]
    private val originalNamedUi: Any? = defaults[uiKey]
    private val originalLafUi: Any? = lafDefaults["ProgressBarUI"]
    private val originalLafNamedUi: Any? = lafDefaults[uiKey]

    init {
        installCustomProgressBarUi()
        connectSettingUpdates()
    }

    private fun installCustomProgressBarUi() {
        val uiClass = EmojiProgressBarUi::class.java

        defaults["ProgressBarUI"] = uiClass
        defaults[uiKey] = uiClass
        lafDefaults["ProgressBarUI"] = uiClass
        lafDefaults[uiKey] = uiClass

        forceRefreshOfOpenProgressBars()
    }

    private fun connectSettingUpdates() {
        val connection = ApplicationManager.getApplication().messageBus.connect(this)
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
        if (ApplicationManager.getApplication().isDisposed) return
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
        restoreOriginal("ProgressBarUI", originalProgressBarUi)
        restoreOriginal(uiKey, originalNamedUi)
        restoreOriginal(lafDefaults, "ProgressBarUI", originalLafUi)
        restoreOriginal(lafDefaults, uiKey, originalLafNamedUi)
        forceRefreshOfOpenProgressBars()
    }

    private fun restoreOriginal(key: String, value: Any?) {
        restoreOriginal(defaults, key, value)
    }

    private fun restoreOriginal(table: UIDefaults, key: String, value: Any?) {
        if (value != null) {
            table[key] = value
        } else {
            table.remove(key)
        }
    }
}
