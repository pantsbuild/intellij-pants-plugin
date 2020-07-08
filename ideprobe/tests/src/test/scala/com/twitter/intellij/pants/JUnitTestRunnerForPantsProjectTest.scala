package com.twitter.intellij.pants

import java.nio.file.Path
import java.nio.file.Paths

import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.protocol.JUnitRunConfiguration
import org.virtuslab.ideprobe.protocol.ModuleRef
import org.junit.Assert._
import org.junit.Test

class JUnitTestRunnerForPantsProjectTest extends PantsTestSuite with BspFixture {

  @Test
  def withBspPlugin(): Unit = fixtureFromConfig().run { intellij =>
    checkTesting(
      intellij,
      modulesWithTests = intellij.config[List[String]]("modulesWithTests.bsp"),
      projectPath = runFastpassCreate(intellij.config, intellij.workspace, intellij.config[List[String]]("targetSpecs"))
    )
  }

  @Test def withPantsPlugin(): Unit = fixtureFromConfig().run { intellij =>
    val pantsCommand = Seq("./pants", "idea-plugin", "--open-with=echo") ++ intellij.config[List[String]]("targetSpecs")
    val projectPath = Paths.get(Shell.run(intellij.workspace, pantsCommand: _*).out)
    checkTesting(
      intellij,
      modulesWithTests = intellij.config[List[String]]("modulesWithTests.pants"),
      projectPath
    )
  }

  private def checkTesting(intellij: RunningIntelliJFixture, modulesWithTests: Seq[String], projectPath: Path): Unit = {
    val project = intellij.probe.openProject(projectPath)
    modulesWithTests.foreach { moduleName =>
      val runConfiguration = JUnitRunConfiguration.module(ModuleRef(moduleName))
      val result = intellij.probe.run(runConfiguration)
      assertTrue(s"There were no suites in $moduleName", result.suites.nonEmpty)
      assertTrue(s"Tests failed in $moduleName with $result", result.isSuccess)
    }
  }

}