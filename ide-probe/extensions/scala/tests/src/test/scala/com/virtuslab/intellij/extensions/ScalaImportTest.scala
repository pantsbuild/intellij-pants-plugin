package com.twitter.intellij.extensions

import com.twitter.ideprobe.Config
import com.twitter.ideprobe.Extensions._
import com.twitter.ideprobe.IntegrationTestSuite
import com.twitter.ideprobe.IntelliJFixture
import com.twitter.ideprobe.Shell
import org.junit.Assert
import org.junit.Test

final class ScalaImportTest extends IntegrationTestSuite {

  private val config = Config.fromClasspath("SbtProject/ideprobe.conf")

  @Test
  def importSbtProject(): Unit = {
    IntelliJFixture.fromConfig(config).run { intellij =>
      val projectRef = intellij.probe.openProject(intellij.workspace.resolve("root"))
      val project = intellij.probe.projectModel(projectRef)
      val modules = project.modules.map(_.name).toSet
      Assert.assertEquals(Set("hello-world-build", "hello-world", "foo", "bar"), modules)
      Assert.assertEquals(project.name, "hello-world")
    }
  }

  @Test
  def importBspProject(): Unit = {
    IntelliJFixture.fromConfig(config).run { intellij =>
      // Root directory is necessary, since bsp plugin creates project
      // inside the **parent** of the directory specified
      val root = intellij.workspace.resolve("root")

      root
        .resolve("project/plugins.sbt")
        .write("""addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.0-RC1")""")

      root
        .resolve(".bsp/bloop.json")
        .write(s"""|{
                   |  "name": "Bloop",
                   |  "version": "1.4.0-RC1",
                   |  "bspVersion": "2.0.0-M4+10-61e61e87",
                   |  "languages": ["scala", "java"],
                   |  "argv": [
                   |    "${intellij.workspace.resolve("bin/coursier")}",
                   |    "launch",
                   |    "ch.epfl.scala:bloop-launcher-core_2.12:1.4.0-RC1",
                   |    "--",
                   |    "1.4.0-RC1"
                   |  ]
                   |}
                   |""".stripMargin)

      val result = Shell.run(in = root, "sbt", "bloopInstall")
      Assert.assertEquals(result.exitCode, 0)
      root.resolve("build.sbt").delete()
      root.resolve("project").delete()

      val projectRef = intellij.probe.openProject(root)
      val project = intellij.probe.projectModel(projectRef)

      val workspaceName = root.getFileName.toString
      val modules = project.modules.map(_.name).toSet
      Assert.assertEquals(Set("foo", "bar", workspaceName), modules)
      Assert.assertEquals(workspaceName, project.name)
    }
  }
}
