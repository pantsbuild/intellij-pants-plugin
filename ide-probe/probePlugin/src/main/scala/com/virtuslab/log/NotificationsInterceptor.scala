package com.twitter.log

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.twitter.handlers.IntelliJApi
import com.twitter.ideprobe.protocol.IdeMessage

object NotificationsInterceptor extends IntelliJApi {

  private class Interceptor extends Notifications {
    override def notify(notification: Notification): Unit = {
      val level = notification.getType match {
        case NotificationType.ERROR       => IdeMessage.Level.Error
        case NotificationType.WARNING     => IdeMessage.Level.Warn
        case NotificationType.INFORMATION => IdeMessage.Level.Info
      }
      val error = Message(Some(notification.getContent), throwable = None, level)
      MessageLog.add(error)
    }
  }

  def inject(): Unit = {
    val bus = application.getMessageBus.connect()
    bus.subscribe(Notifications.TOPIC, new Interceptor)
    bus.subscribe(
      ProjectManager.TOPIC,
      new ProjectManagerListener {
        override def projectOpened(project: Project): Unit = {
          project.getMessageBus.connect().subscribe(Notifications.TOPIC, new Interceptor)
        }
      }
    )
  }
}
