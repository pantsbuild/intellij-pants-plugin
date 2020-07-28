package com.twitter.intellij.pants

import java.nio.file.Path
import java.nio.file.Paths

import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.Shell.CommandResult

trait PantsFixture {
  def runPants(workspace: Path, command: Seq[String]): CommandResult = {
    val pantsCommand = "./pants" +: command
    Shell.run(workspace, pantsCommand: _*)
  }

  def runPantsIdeaPlugin(workspace: Path, targets: Seq[String]): Path = {
    val args = Seq("idea-plugin", "--open-with=echo") ++ targets
    Paths.get(runPants(workspace, args).out)
  }
}
