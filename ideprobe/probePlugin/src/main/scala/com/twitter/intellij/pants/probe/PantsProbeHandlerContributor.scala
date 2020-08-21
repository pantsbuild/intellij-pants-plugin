package com.twitter.intellij.pants.probe

import com.twitter.intellij.pants.probe.handlers.PantsImport
import com.twitter.intellij.pants.probe.handlers.PantsSettings
import com.twitter.intellij.pants.probe.handlers.PythonProject
import com.twitter.intellij.pants.protocol.PantsEndpoints
import org.virtuslab.ProbeHandlerContributor
import org.virtuslab.ProbeHandlers.ProbeHandler

class PantsProbeHandlerContributor extends ProbeHandlerContributor {
  override def registerHandlers(handler: ProbeHandler): ProbeHandler = {
    handler
      .on(PantsEndpoints.ImportPantsProject)((PantsImport.importProject _).tupled)
      .on(PantsEndpoints.ChangePantsProjectSettings)((PantsSettings.changeProjectSettings _).tupled)
      .on(PantsEndpoints.GetPantsProjectSettings)(PantsSettings.getProjectSettings)
      .on(PantsEndpoints.GetPythonFacets)(PythonProject.facets)
  }
}
