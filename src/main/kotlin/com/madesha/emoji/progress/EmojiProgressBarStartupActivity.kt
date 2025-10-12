package com.madesha.emoji.progress

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.madesha.emoji.progress.ui.EmojiProgressBarManager

class EmojiProgressBarStartupActivity : StartupActivity, DumbAware {
    override fun runActivity(project: Project) {
        val application = ApplicationManager.getApplication()
        if (application.isDisposed) return

        val manager = application.getService(EmojiProgressBarManager::class.java)
        manager.initialize()
    }
}

