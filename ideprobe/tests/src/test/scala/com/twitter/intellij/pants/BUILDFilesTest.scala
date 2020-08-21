package com.twitter.intellij.pants

import org.junit.Assert.assertEquals
import org.junit.Test
import org.virtuslab.ideprobe.Assertions
import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.protocol.FileRef
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.Reference

final class BUILDFilesTest extends PantsTestSuite with Assertions {

  @Test
  def referencesOtherBUILDFilesInBsp(): Unit = {
    checkReferencesToOtherBUILDFiles(openProjectWithBsp)
  }

  @Test
  def referencesOtherBUILDFilesInPants(): Unit = {
    checkReferencesToOtherBUILDFiles(openProjectWithPants)
  }

  def checkReferencesToOtherBUILDFiles(openProject: RunningIntelliJFixture => ProjectRef): Unit = {
    fixtureFromConfig().run { intellij =>
      val project = openProject(intellij)

      def file(name: String): FileRef = FileRef(intellij.workspace.resolve(name), project)

      val thriftLibBuild = file("thrift-lib/BUILD")
      val thirdPartyBuild = file("3rdParty/BUILD")
      val javaAppBuild = file("java-app/BUILD")

      val refs = intellij.probe.fileReferences(javaAppBuild)

      assertEquals(2, refs.size)
      assertContains(refs)(
        Reference("3rdParty", Reference.Target.File(thirdPartyBuild)),
        Reference("thrift-lib", Reference.Target.File(thriftLibBuild))
      )
    }
  }

}
