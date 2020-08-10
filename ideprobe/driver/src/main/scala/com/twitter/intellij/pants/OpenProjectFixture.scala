package com.twitter.intellij.pants

import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.protocol.ProjectRef

trait OpenProjectFixture extends PantsFixture with BspFixture {

  def openProjectWithBsp(intelliJ: RunningIntelliJFixture): ProjectRef = {
    val projectPath = runFastpassCreate(intelliJ.config, intelliJ.workspace, targetsFromConfig(intelliJ))
    intelliJ.probe.openProject(projectPath)
  }

  def openProjectWithPants(intelliJ: RunningIntelliJFixture): ProjectRef = {
    val projectPath = runPantsIdeaPlugin(intelliJ.workspace, targetsFromConfig(intelliJ))
    intelliJ.probe.openProject(projectPath)
  }

  private def targetsFromConfig(intelliJ: RunningIntelliJFixture): Seq[String] =  {
    intelliJ.config[Seq[String]]("pants.import.targets")
  }
}
