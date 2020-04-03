package com.twitter.intellij.pants

import com.twitter.intellij.pants.protocol.PantsProjectSettingsChangeRequest
import com.twitter.ideprobe.protocol.Setting
import org.junit.Assert._
import org.junit.Test

class PantsImportWizardTest extends PantsTestSuite {

  @Test def importProjectWithCustomSettings(): Unit = {
    fixtureFromConfig().run { intelliJ =>
      val importSettings =
        PantsProjectSettingsChangeRequest(
          loadSourcesAndDocsForLibs = Setting.Changed(true),
          useIntellijCompiler = Setting.Changed(true)
        )

      val projectRoot = "examples/src/java/org/pantsbuild/example/hello"
      val project = intelliJ.probe.importProject(intelliJ.workspace.resolve(projectRoot), importSettings)

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
