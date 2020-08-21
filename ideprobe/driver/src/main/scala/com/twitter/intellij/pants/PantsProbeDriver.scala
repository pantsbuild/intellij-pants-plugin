package com.twitter.intellij.pants

import java.nio.file.Path

import com.twitter.intellij.pants.protocol.PantsEndpoints
import com.twitter.intellij.pants.protocol.PantsProjectSettings
import com.twitter.intellij.pants.protocol.PantsProjectSettingsChangeRequest
import com.twitter.intellij.pants.protocol.PythonFacet
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.protocol.IdeNotification
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.RobotExtensions._
import org.virtuslab.ideprobe.protocol.ModuleRef

import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object PantsProbeDriver {
  val pluginId = "org.virtuslab.ideprobe.pants"

  def apply(driver: ProbeDriver): PantsProbeDriver = driver.as(pluginId, new PantsProbeDriver(_))
}

final class PantsProbeDriver(val driver: ProbeDriver) extends AnyVal {
  def getPythonFacets(moduleRef: ModuleRef): Seq[PythonFacet] = {
    driver.send(PantsEndpoints.GetPythonFacets, moduleRef)
  }

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

  def compileAllTargets(timeout: Duration = 10.minutes): PantsBuildResult = {
    driver.invokeAction("com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsAction")
    val compiledNotification = Try(driver.awaitNotification("Compile message", timeout))

    val output = (for {
      panel <- driver.robot.findOpt(query.className("PantsConsoleViewPanel"))
      editor <- panel.findOpt(query.className("EditorComponentImpl"))
    } yield editor.fullText).getOrElse("<output not found>")

    compiledNotification match {
      case Success(notification) if notification.severity == IdeNotification.Severity.Info =>
        PantsBuildResult(PantsBuildResult.Status.Passed, output)
      case Success(_) =>
        PantsBuildResult(PantsBuildResult.Status.Failed, output)
      case Failure(exception) =>
        exception.printStackTrace()
        PantsBuildResult(PantsBuildResult.Status.Timeout, output)
    }
  }
}
