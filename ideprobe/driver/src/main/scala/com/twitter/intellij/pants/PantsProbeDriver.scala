package com.twitter.intellij.pants

import java.nio.file.Path
import com.twitter.intellij.pants.protocol.PantsEndpoints
import com.twitter.intellij.pants.protocol.PantsProjectSettings
import com.twitter.intellij.pants.protocol.PantsProjectSettingsChangeRequest
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.protocol.IdeNotification
import org.virtuslab.ideprobe.protocol.ProjectRef

object PantsProbeDriver {
  val pluginId = "org.virtuslab.ideprobe.pants"

  def apply(driver: ProbeDriver): PantsProbeDriver = driver.as(pluginId, new PantsProbeDriver(_))
}

final class PantsProbeDriver(val driver: ProbeDriver) extends AnyVal {
  def importProject(path: Path, settings: PantsProjectSettingsChangeRequest): ProjectRef = {
    driver.send(PantsEndpoints.ImportPantsProject, (path, settings))
  }

  def getPantsProjectSettings(project: ProjectRef = ProjectRef.Default): PantsProjectSettings = {
    driver.send(PantsEndpoints.GetPantsProjectSettings, project)
  }

  def setPantsProjectSettings(
      settings: PantsProjectSettingsChangeRequest,
      project: ProjectRef = ProjectRef.Default
  ): Unit = {
    driver.send(PantsEndpoints.ChangePantsProjectSettings, (project, settings))
  }

  def compileAllTargets(): Unit = {
    driver.invokeAction("com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsAction")
    val compiledNotification = driver.awaitNotification("Compile message")

    if (compiledNotification.severity != IdeNotification.Severity.Info) {
      throw new IllegalStateException("Compilation failed")
    }
  }
}
