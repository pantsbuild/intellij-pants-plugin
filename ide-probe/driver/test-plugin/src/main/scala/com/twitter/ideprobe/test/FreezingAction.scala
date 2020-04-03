package com.twitter.ideprobe.test

import java.util.concurrent.TimeUnit
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class FreezingAction extends AnAction("Freeze Action") with DumbAware {
  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {
    TimeUnit.SECONDS.sleep(10)
  }
}
