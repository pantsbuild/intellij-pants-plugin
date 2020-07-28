package com.twitter.intellij.pants

import org.junit.Test
import org.virtuslab.ideprobe.Assertions
import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.protocol.NavigationQuery
import org.virtuslab.ideprobe.protocol.NavigationTarget
import org.virtuslab.ideprobe.protocol.ProjectRef

final class ThriftIdeaPluginTest extends PantsTestSuite with Assertions {

  @Test
  def findThriftFiles(): Unit = {
    findThriftFiles(openProjectWithPants)
  }

  @Test
  def findThriftFilesBsp(): Unit = {
    findThriftFiles(openProjectWithBsp)
  }

  def findThriftFiles(openProject: RunningIntelliJFixture => ProjectRef): Unit = {
    fixtureFromConfig().run { intelliJ =>
      openProject(intelliJ)
      val targets = intelliJ.probe.find(NavigationQuery("ThriftStruct", includeNonProjectItems = true))
      assertContains(targets)(
        NavigationTarget("ThriftStruct", "(foo.bar)"), // generated java file
        NavigationTarget("ThriftStruct", "struct.thrift") // original thrift source
      )
    }
  }
}