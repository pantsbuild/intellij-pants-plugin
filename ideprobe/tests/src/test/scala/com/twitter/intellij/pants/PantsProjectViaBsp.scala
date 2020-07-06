package com.twitter.intellij.pants

import org.virtuslab.ideprobe.Assertions
import org.virtuslab.ideprobe.Config
import org.junit.Assert
import org.junit.Test

class PantsProjectViaBsp extends PantsTestSuite with Assertions with BspFixture {
  @Test
  def importPantsBspProject(): Unit = {
    fixtureFromConfig().run { intellij =>
      val projectPath = runFastpassCreate(intellij.config, intellij.workspace, targets = Seq("java_app::"))
      val projectRef = intellij.probe.openProject(projectPath)
      val project = intellij.probe.projectModel(projectRef)
      Assert.assertEquals("java_app", project.name)
      Assert.assertEquals(List("java_app:main-bin", "java_app-root"), project.modules.map(_.name).toList)
    }
  }
}
