package com.twitter.ideprobe.test

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class ThrowingAction extends AnAction("Throwing Action") with DumbAware {
  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {
    throw new RuntimeException("Test failure from ThrowingAction")
  }
}
