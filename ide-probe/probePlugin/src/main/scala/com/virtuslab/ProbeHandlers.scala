package com.twitter

import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.PluginDescriptor
import com.twitter.ideprobe.jsonrpc.JsonRpc

trait ProbeHandlerContributor {
  def registerHandlers(handler: JsonRpc.Handler): JsonRpc.Handler
}

object ProbeHandlers {
  val EP_NAME = ExtensionPointName.create[ProbeHandlerContributor]("com.twitter.ideprobe.probeHandlerContributor")

  private var handler: JsonRpc.Handler = collectHandlers()

  EP_NAME.addExtensionPointListener(
    new ExtensionPointListener[ProbeHandlerContributor] {
      override def extensionAdded(extension: ProbeHandlerContributor, pluginDescriptor: PluginDescriptor): Unit =
        handler = collectHandlers()

      override def extensionRemoved(extension: ProbeHandlerContributor, pluginDescriptor: PluginDescriptor): Unit =
        handler = collectHandlers()
    },
    null
  )

  def get(): JsonRpc.Handler = handler

  private def collectHandlers() = {
    val handler = new JsonRpc.Handler()
    EP_NAME.getExtensions().foldLeft(handler)((handler, contributor) => contributor.registerHandlers(handler))
  }
}
