package com.madesha.emoji.progress

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.madesha.emoji.progress.ui.EmojiProgressBarManager

class EmojiProgressBarAppLifecycleListener : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        ApplicationManager.getApplication().getService(EmojiProgressBarManager::class.java)
    }
}

