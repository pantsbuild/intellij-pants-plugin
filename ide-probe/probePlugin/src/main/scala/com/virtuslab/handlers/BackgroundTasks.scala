package com.twitter.handlers

import java.lang.reflect.Method

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object BackgroundTasks extends IntelliJApi {
  def withAwaitNone[A](block: => A): A = {
    val result = block
    awaitNone()
    result
  }

  def awaitNone(): Unit = {
    doWait()
  }

  @tailrec
  private def doWait(): Unit = {
    sleep(5.seconds)
    val tasks = currentBackgroundTasks()
    if (tasks.nonEmpty) {
      log.warn(s"Waiting for completion of $tasks")
      doWait()
    } else {
      if (newTaskAppeared(within = 2.seconds, probeFrequency = 50.millis)) {
        doWait()
      }
    }
  }

  @tailrec
  private def newTaskAppeared(within: FiniteDuration, probeFrequency: FiniteDuration): Boolean = {
    if (within <= 0.millis) {
      false
    } else {
      sleep(probeFrequency)
      val tasks = currentBackgroundTasks()
      if (tasks.nonEmpty) {
        true
      } else {
        newTaskAppeared(within - probeFrequency, probeFrequency)
      }
    }
  }

  private def currentBackgroundTasks(): Seq[ProgressIndicator] = {
    val progressManager = ProgressManager.getInstance
    getCurrentIndicatorsMethod.invoke(progressManager).asInstanceOf[java.util.List[ProgressIndicator]].asScala.toSeq
  }

  private lazy val getCurrentIndicatorsMethod = {
    val method = getMethod(ProgressManager.getInstance.getClass, "getCurrentIndicators")
    method.setAccessible(true)
    method
  }

  @tailrec private def getMethod(cls: Class[_], name: String): Method = {
    try cls.getDeclaredMethod(name)
    catch {
      case e: NoSuchMethodException =>
        if (cls.getSuperclass != null) {
          getMethod(cls.getSuperclass, name)
        } else throw e
    }
  }

  private def sleep(duration: FiniteDuration): Unit = {
    Thread.sleep(duration.toMillis)
  }
}
