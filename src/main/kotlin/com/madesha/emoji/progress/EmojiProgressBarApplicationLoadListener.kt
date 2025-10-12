package com.madesha.emoji.progress

import com.intellij.ide.ApplicationLoadListener
import com.intellij.openapi.application.Application
import com.madesha.emoji.progress.ui.EmojiProgressBarManager
import java.nio.file.Path

class EmojiProgressBarApplicationLoadListener : ApplicationLoadListener {
    override fun beforeApplicationLoaded(application: Application, configPath: Path) {
        if (application.isDisposed) return
        val manager = application.getService(EmojiProgressBarManager::class.java)
        manager.initialize()
    }
}
