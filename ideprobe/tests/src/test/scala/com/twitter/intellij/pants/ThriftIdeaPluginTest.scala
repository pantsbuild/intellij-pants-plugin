package com.twitter.intellij.pants

import org.virtuslab.ideprobe.Assertions
import org.virtuslab.ideprobe.protocol.ModuleRef
import org.virtuslab.ideprobe.protocol.NavigationQuery
import org.virtuslab.ideprobe.protocol.NavigationTarget
import org.virtuslab.ideprobe.protocol.ApplicationRunConfiguration
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.junit.{Assert, Ignore, Test}
import org.junit.Assert.assertTrue

final class ThriftIdeaPluginTest extends PantsTestSuite with Assertions {

  @Test
  def runsMainClass(): Unit = {
    fixtureFromConfig().run { intelliJ =>
      PantsProbeDriver(intelliJ.probe).compileAllTargets()

      val runConfig = ApplicationRunConfiguration(
        ModuleRef("java-app_main"),
        mainClass = "Main"
      )
      val result = intelliJ.probe.run(runConfig)

      assertTrue(result.finishedSuccessfully)
      Assert.assertEquals("ThriftStruct()\n", result.stdout)
    }
  }

  @Test
  def findThriftFiles(): Unit = fixtureFromConfig().run { intelliJ =>
    val targets = intelliJ.probe.find(NavigationQuery(value = "ThriftStruct"))
    assertContains(targets)(
      NavigationTarget("ThriftStruct", "(foo.bar)"), // generated java file
      NavigationTarget("ThriftStruct", "struct.thrift") // original thrift source
    )
  }
}