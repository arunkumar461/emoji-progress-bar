package com.madesha.emoji.progress

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.ApplicationManager
import com.madesha.emoji.progress.ui.EmojiProgressBarManager
import kotlinx.coroutines.CoroutineScope

class EmojiProgressBarInitializedListener : ApplicationInitializedListener {
    override suspend fun execute(coroutineScope: CoroutineScope) {
        val application = ApplicationManager.getApplication()
        if (application.isDisposed) return

        val manager = application.getService(EmojiProgressBarManager::class.java)
        manager.initialize()
    }
}

