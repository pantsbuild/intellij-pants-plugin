package com.twitter.handlers

import java.lang.reflect.Field
import java.nio.file.Path
import java.nio.file.Paths

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.ui.CheckBoxList
import com.twitter.Probe
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import javax.swing.JList

import scala.annotation.tailrec
import scala.reflect.ClassTag

trait IntelliJApi {
  def runOnUIAsync(block: => Unit): Unit = {
    application.invokeLater(() => block)
  }

  def runOnUISync[A](block: => A): A = {
    var result = Option.empty[A]
    application.invokeAndWait(() => result = Some(block))
    result.get
  }

  def write[A](block: => A): A = {
    application.runWriteAction(new ThrowableComputable[A, Throwable] {
      override def compute(): A = block
    })
  }

  def read[A](block: => A): A = {
    application.runReadAction(new ThrowableComputable[A, Throwable] {
      override def compute(): A = block
    })
  }

  protected val log: Logger = Logger.getInstance(classOf[Probe])

  protected def logsPath: Path = Paths.get(PathManager.getLogPath)

  protected def application: Application = ApplicationManager.getApplication

  protected def error(message: String): Nothing = {
    throw new RuntimeException(message)
  }

  protected def await[A](future: Future[A]): A = {
    Await.result(future, Duration.Inf)
  }

  implicit class UserDataHolderOps(val holder: UserDataHolder) {
    def getAndClearUserData[A](key: Key[A]): A = {
      val result = holder.getUserData(key)
      holder.putUserData(key, null.asInstanceOf[A])
      result
    }
  }

  implicit class ReflectionOps[A](obj: A) {
    def field[B: ClassTag](name: String): B = {
      val field = getField(obj.getClass, "my" + name.capitalize)
      field.setAccessible(true)
      field.get(obj).asInstanceOf[B]
    }

    @tailrec
    private def getField(cl: Class[_], name: String): Field = {
      try cl.getDeclaredField(name)
      catch {
        case e: NoSuchFieldException =>
          if (cl.getSuperclass != null) getField(cl.getSuperclass, name) else throw e
      }
    }
  }

  implicit class JListOps[A](list: JList[A]) {
    def items: Seq[A] = {
      val listModel = list.getModel
      (0 until listModel.getSize).map(listModel.getElementAt)
    }
  }

  implicit class CheckboxListOps[A](list: CheckBoxList[A]) {
    def items: Seq[A] = {
      (0 until list.getItemsCount).map(list.getItemAt)
    }
  }
}
