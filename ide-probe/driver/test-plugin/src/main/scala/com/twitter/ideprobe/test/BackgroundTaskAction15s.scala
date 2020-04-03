package com.twitter.ideprobe.test

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware

class BackgroundTaskAction15s extends AnAction("Background Task Action") with DumbAware {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val task = new Task.Backgroundable(e.getProject, "Doing something for 15s", true) {
      override def run(indicator: ProgressIndicator): Unit = {
        indicator.setText("Troubleshooting...")
        for (_ <- 1 to 15) {
          indicator.checkCanceled()
          Thread.sleep(1000)
        }
      }
    }
    ProgressManager.getInstance().run(task)
  }
}
