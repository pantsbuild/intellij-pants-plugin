package com.twitter

import com.intellij.ide.ApplicationInitializedListener
import com.twitter.log.IdeaLogInterceptor
import com.twitter.log.NotificationsInterceptor

/**
 * Starts the IdeProbe on startup
 */
final class IdeProbeLauncher extends ApplicationInitializedListener {
  override def componentsInitialized(): Unit = {
    IdeaLogInterceptor.inject()
    NotificationsInterceptor.inject()
    IdeProbeService().start()
  }
}
