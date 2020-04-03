package com.twitter.intellij.pants.probe

import com.twitter.intellij.pants.probe.handlers.PantsImport
import com.twitter.intellij.pants.probe.handlers.PantsSettings
import com.twitter.intellij.pants.protocol.PantsEndpoints
import com.twitter.ProbeHandlerContributor
import com.twitter.ideprobe.jsonrpc.JsonRpc

class PantsProbeHandlerContributor extends ProbeHandlerContributor {
  override def registerHandlers(handler: JsonRpc.Handler): JsonRpc.Handler = {
    handler
      .on(PantsEndpoints.ImportPantsProject)((PantsImport.importProject _).tupled)
      .on(PantsEndpoints.ChangePantsProjectSettings)((PantsSettings.changeProjectSettings _).tupled)
      .on(PantsEndpoints.GetPantsProjectSettings)(PantsSettings.getProjectSettings)
  }
}
