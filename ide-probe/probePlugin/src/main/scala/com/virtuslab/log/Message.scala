package com.twitter.log

import com.intellij.idea.IdeaLogger
import com.twitter.ideprobe.protocol.IdeMessage

case class Message(content: Option[String], throwable: Option[Throwable], level: IdeMessage.Level) {

  def throwableText: Option[String] = {
    throwable.map(IdeaLogger.getThrowableRenderer.doRender(_).mkString("\n"))
  }

  def render: String = {
    (content ++ throwableText).mkString("\n\n")
  }
}
