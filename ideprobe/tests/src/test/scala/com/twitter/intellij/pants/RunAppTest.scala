package com.twitter.intellij.pants

import org.junit.Assert
import org.junit.Test
import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.ide.intellij.IntelliJPaths
import org.virtuslab.ideprobe.protocol.ApplicationRunConfiguration
import org.virtuslab.ideprobe.protocol.ProjectRef

class RunAppTest extends PantsTestSuite with ConfigFormat {

  @Test
  def runsMainClassWithPants(): Unit = {
    runsMainClass("pants", openProjectWithPants, _.probe.compileAllTargets().assertSuccess())
  }

  @Test
  def runsMainClassWithBsp(): Unit = {
    runsMainClass("bsp", openProjectWithBsp, _.probe.build().assertSuccess())
  }

  def runsMainClass(
    configSuffix: String,
    openProject: RunningIntelliJFixture => ProjectRef,
    buildProject: RunningIntelliJFixture => Unit,
  ): Unit = {
    import pureconfig.generic.auto._
    fixtureFromConfig().run { intelliJ =>
      openProject(intelliJ)
      buildProject(intelliJ)

      val runConfig = intelliJ.config[ApplicationRunConfiguration](s"runConfiguration.$configSuffix")
      val result = intelliJ.probe.run(runConfig)

      Assert.assertTrue(result.finishedSuccessfully)
      Assert.assertEquals(intelliJ.config[String]("expectedStdout"), result.stdout)
    }
  }

}
