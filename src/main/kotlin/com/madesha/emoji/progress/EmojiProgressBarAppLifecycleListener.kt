package com.madesha.emoji.progress

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.madesha.emoji.progress.ui.EmojiProgressBarManager

class EmojiProgressBarAppLifecycleListener : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        val application = ApplicationManager.getApplication()
        if (application.isDisposed) return

        val manager = application.getService(EmojiProgressBarManager::class.java)
        manager.initialize()
    }
}

