package com.twitter.ideprobe

import com.twitter.ideprobe.Extensions._
import com.twitter.ideprobe.dependencies.IntelliJVersion
import com.twitter.ideprobe.dependencies.Plugin
import com.twitter.ideprobe.protocol.BuildScope
import com.twitter.ideprobe.protocol.InstalledPlugin
import com.twitter.ideprobe.protocol.ProjectRef
import org.junit.Assert._
import org.junit.Test
import scala.concurrent.duration._

final class ProbeDriverTest extends IntegrationTestSuite with Assertions {
  private val scalaPlugin = Plugin("org.intellij.scala", "2020.1.10")
  private val probeTestPlugin = ProbeTestPlugin.bundled

  private val fixture = IntelliJFixture(
    version = IntelliJVersion.Latest,
    plugins = List(scalaPlugin, probeTestPlugin)
  )

  @Test
  def listsPlugins(): Unit = fixture.run { intelliJ =>
    val plugins = intelliJ.probe.plugins

    assertContains(plugins)(InstalledPlugin(scalaPlugin.id, scalaPlugin.version))
    assertExists(plugins)(plugin => plugin.id == ProbeTestPlugin.id)
  }

  @Test
  def collectErrors(): Unit = fixture.run { intelliJ =>
    intelliJ.probe.invokeActionAsync("com.twitter.ideprobe.test.ThrowingAction")
    val errors = intelliJ.probe.errors
    assertExists(errors)(error =>
      error.content.contains("ThrowingAction") && error.pluginId.contains(ProbeTestPlugin.id)
    )
  }

  @Test
  def projectOpen(): Unit =
    fixture
      .copy(workspaceTemplate = WorkspaceTemplate.FromResource("OpenProjectTest"))
      .run { intelliJ =>
        val expectedProjectName = "empty-project"
        val projectPath = intelliJ.workspace.resolve(expectedProjectName)
        val actualProjectRef = intelliJ.probe.openProject(projectPath)
        assertEquals(ProjectRef(expectedProjectName), actualProjectRef)
      }

  @Test
  def backgroundTask(): Unit = fixture.run { intelliJ =>
    // time it takes to verify that IDE is actually idle
    val errorMargin = 15.seconds

    assertDuration(min = 15.seconds, max = 5.minutes) {
      intelliJ.probe.invokeAction("com.twitter.ideprobe.test.BackgroundTaskAction15s")
    }
    assertDuration(max = errorMargin) {
      intelliJ.probe.awaitIdle()
    }
  }

  @Test
  def freezeInspector(): Unit = fixture.withDisplay().run { intelliJ =>
    intelliJ.probe.invokeAction("com.twitter.ideprobe.test.FreezingAction")
    val freezes = intelliJ.probe.freezes
    assertExists(freezes) { freeze =>
      freeze.duration.exists(_ >= 10.seconds) &&
      freeze.edtStackTrace.exists(frame => frame.contains("Thread.sleep")) &&
      freeze.edtStackTrace.exists(frame => frame.contains("FreezingAction.actionPerformed"))
    }
  }

  private val buildTestFixture = fixture
    .withDisplay()
    .copy(workspaceTemplate = WorkspaceTemplate.FromResource("BuildTest"))

  @Test def buildProjectTest(): Unit = {
    buildTestFixture
      .run { intelliJ =>
        val projectDir = intelliJ.workspace.resolve("simple-sbt-project")

        intelliJ.probe.openProject(projectDir)

        val successfulResult = intelliJ.probe.build()
        assertEquals(successfulResult.errors.toSeq, Nil)

        projectDir.resolve("src/main/scala/Main.scala").write("Not valid scala")
        intelliJ.probe.syncFiles()

        val failedResult = intelliJ.probe.build()
        assertExists(failedResult.errors) { error =>
          error.file.exists(_.endsWith("src/main/scala/Main.scala")) &&
          error.content.contains("expected class or object definition")
        }
      }
  }

  @Test def buildFilesTest(): Unit = {
    buildTestFixture.run { intelliJ =>
      val projectDir = intelliJ.workspace.resolve("simple-sbt-project")
      val project = intelliJ.probe.openProject(projectDir)

      val compilingFile = projectDir.resolve("src/main/scala/Main.scala")
      val nonCompilingFile = projectDir.resolve("src/main/scala/Incorrect.scala")
      nonCompilingFile.write("incorrect")
      intelliJ.probe.syncFiles()

      val failedResult = intelliJ.probe.build(BuildScope.files(project, nonCompilingFile))
      val successfulResult = intelliJ.probe.build(BuildScope.files(project, compilingFile))

      assertExists(failedResult.errors)(error => error.file.exists(_.endsWith("Incorrect.scala")))
      assertEquals(Nil, successfulResult.errors)
    }
  }

  @Test
  def startsUsingCustomCommand(): Unit = {
    IntelliJFixture.fromConfig(Config.fromClasspath("CustomCommand/ideprobe.conf")).run { intelliJ =>
      val output = intelliJ.workspace.resolve("output")
      assertTrue(s"Not a file $output", output.isFile)
    }
  }
}
