package com.twitter.intellij.pants

import com.twitter.ideprobe.Assertions
import com.twitter.ideprobe.protocol.ModuleRef
import com.twitter.ideprobe.protocol.NavigationQuery
import com.twitter.ideprobe.protocol.NavigationTarget
import com.twitter.ideprobe.protocol.RunConfiguration
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test

final class ThriftIdeaPluginTest extends PantsTestSuite with Assertions {

  @Test
  def runsMainClass(): Unit = {
    val runConfig = RunConfiguration(ModuleRef("java.app:main", "java-app_main"), fqn = "Main")

    fixtureFromConfig().run { intelliJ =>
      PantsProbeDriver(intelliJ.probe).compileAllTargets()

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
