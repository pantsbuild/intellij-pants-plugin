package com.twitter.handlers

import com.intellij.notification.Notification
import com.intellij.notification.{Notifications => NotificationListener}
import com.intellij.openapi.application.ApplicationManager
import com.twitter.ideprobe.protocol.IdeNotification
import scala.concurrent.Promise

object Notifications extends IntelliJApi {
  def await(id: String): IdeNotification = {
    val result = Promise[IdeNotification]()

    ApplicationManager.getApplication.getMessageBus
      .connect()
      .subscribe(
        NotificationListener.TOPIC,
        new NotificationListener {
          override def notify(notification: Notification): Unit = {
            if (notification.getTitle == id) {
              result.success(new IdeNotification(notification.getType.name()))
            }
          }
        }
      )
    await(result.future)
  }
}
