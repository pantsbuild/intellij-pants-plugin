package com.twitter.handlers

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.ProjectManager

object Actions extends IntelliJApi {

  def invokeAsync(id: String): Unit =
    BackgroundTasks.withAwaitNone {
      val action = getAction(id)
      runOnUIAsync {
        invoke(action)
      }
    }

  def invoke(id: String): Unit =
    BackgroundTasks.withAwaitNone {
      val action = getAction(id)
      runOnUISync {
        invoke(action)
      }
    }

  private def getAction(id: String) = {
    val action = ActionManager.getInstance.getAction(id)
    if (action == null) error(s"Action $id not found")
    action
  }

  private def invoke(action: AnAction): Unit = {
    val event = createDummyEvent(action)
    action.actionPerformed(event)
  }

  private def createDummyEvent(action: AnAction) = {
    val context = new DataContext {
      override def getData(dataId: String): AnyRef = {
        if (dataId == CommonDataKeys.PROJECT.getName) {
          // TODO allow specifying project
          ProjectManager.getInstance.getOpenProjects.headOption.orNull
        } else null
      }
    }
    AnActionEvent.createFromAnAction(action, null, ActionPlaces.ACTION_SEARCH, context)
  }

}
