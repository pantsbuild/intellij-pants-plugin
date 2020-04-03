package com.twitter.handlers

import com.intellij.diagnostic.IdeErrorsDialog
import com.twitter.ideprobe.protocol.IdeMessage
import com.twitter.log.MessageLog

object IdeMessages {
  def list: Array[IdeMessage] = {
    MessageLog.all.map { m =>
      val pluginId = m.throwable.flatMap(t => Option(IdeErrorsDialog.findPluginId(t))).map(_.getIdString)
      val message = m.render
      IdeMessage(m.level, message, pluginId)
    }.toArray
  }
}
