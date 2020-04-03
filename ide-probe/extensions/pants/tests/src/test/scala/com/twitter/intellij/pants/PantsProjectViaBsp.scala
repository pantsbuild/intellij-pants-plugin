package com.twitter.intellij.pants

import com.twitter.ideprobe.Shell
import com.twitter.ideprobe.Assertions
import com.twitter.ideprobe.Config
import com.twitter.ideprobe.IntelliJFixture
import org.junit.Assert
import org.junit.Test

class PantsProjectViaBsp extends PantsTestSuite with Assertions {
  private val configPants = Config.fromClasspath("PantsProject/ideprobe.conf")

  @Test
  def importPantsBspProject: Unit = {
    IntelliJFixture.fromConfig(configPants).run { intellij =>
      val root = intellij.workspace.resolve("root")
      Shell.run(
        in = root,
        root.resolve("coursier").toString,
        "launch",
        "org.scalameta:metals_2.12:0.8.2+6-0bc676db-SNAPSHOT",
        "-r",
        "sonatype:snapshots",
        "--main",
        "scala.meta.internal.pantsbuild.BloopPants",
        "--",
        "create",
        "java_app::"
      )
      val intellijBsp = intellij.workspace.resolve("intellij-bsp").resolve("javaapp").resolve("javaapp")
      val projectRef = intellij.probe.openProject(intellijBsp)
      val project = intellij.probe.projectModel(projectRef)
      Assert.assertEquals("javaapp", project.name)
      Assert.assertEquals(List("java_app:main-bin", "javaapp-root"), project.modules.map(_.name).toList)
    }
  }
}
