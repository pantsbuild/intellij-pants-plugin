package com.twitter.intellij.pants

import org.junit.Assert._
import org.junit.Test
import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.protocol.JUnitRunConfiguration
import org.virtuslab.ideprobe.protocol.ModuleRef
import org.virtuslab.ideprobe.protocol.ProjectRef

class RunTestsTest extends PantsTestSuite with BspFixture {

  @Test def runTestsWithBsp(): Unit = {
    runTests("bsp", openProjectWithBsp)
  }

  @Test def runTestsWithPants(): Unit = {
    runTests("pants", openProjectWithPants)
  }

  private def runTests(
    configSuffix: String,
    openProject: RunningIntelliJFixture => ProjectRef
  ): Unit = fixtureFromConfig().run { intelliJ =>
    openProject(intelliJ)
    val modulesWithTests = intelliJ.config[List[String]](s"modulesWithTests.$configSuffix")
    modulesWithTests.foreach { moduleName =>
      val runConfiguration = JUnitRunConfiguration.module(ModuleRef(moduleName))
      val result = intelliJ.probe.run(runConfiguration)
      assertTrue(s"There were no suites in $moduleName", result.suites.nonEmpty)
      assertTrue(s"Tests failed in $moduleName with $result", result.isSuccess)
    }
  }

}