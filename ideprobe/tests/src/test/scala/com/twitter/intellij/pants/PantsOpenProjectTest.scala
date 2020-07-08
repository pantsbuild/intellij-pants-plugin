package com.twitter.intellij.pants

import java.nio.file.Paths

import com.twitter.intellij.pants.protocol.PantsProjectSettingsChangeRequest
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.Setting
import org.junit.Assert._
import org.junit.Test

class PantsOpenProjectTest extends PantsTestSuite {
  @Test
  def openPantsGeneratedProjectWithValidName(): Unit = {
    fixtureFromConfig().withWorkspace { ws =>
      val projectPath = ws.runShell(Seq("./pants", "idea-plugin", "--open-with=echo", "java_app::")).out

      ws.run { intelliJ =>
        val projectRef = intelliJ.probe.openProject(Paths.get(projectPath))
        assertEquals(ProjectRef("java_app::"), projectRef)
      }
    }
  }

  @Test def importProjectWithCustomSettings(): Unit = {
    fixtureFromConfig().run { intelliJ =>
      val importSettings =
        PantsProjectSettingsChangeRequest(
          loadSourcesAndDocsForLibs = Setting.Changed(true),
          useIntellijCompiler = Setting.Changed(true)
        )

      val projectRoot = intelliJ.workspace.resolve("java_app")
      val project = intelliJ.probe.importProject(projectRoot, importSettings)

      val initialSettings = intelliJ.probe.getPantsProjectSettings()

      assertTrue("'Load sources and docs for libs' was not set", initialSettings.loadSourcesAndDocsForLibs)
      assertTrue("'Use IntelliJ compiler' was not set", initialSettings.useIntellijCompiler)

      intelliJ.probe.setPantsProjectSettings(
        PantsProjectSettingsChangeRequest(useIntellijCompiler = Setting.Changed(false))
      )

      val finalSettings = intelliJ.probe.getPantsProjectSettings()
      assertFalse("'Load sources and docs for libs' was not unset", finalSettings.useIntellijCompiler)
    }
  }

}