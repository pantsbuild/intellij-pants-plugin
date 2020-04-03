package com.twitter.intellij.pants

import java.nio.file.Paths
import com.twitter.ideprobe.IntelliJFixture

class PantsIssue42 extends PantsTestSuite with BugfixTest {

  def reproduce(fixture: IntelliJFixture): Unit = fixture.withWorkspace { ws =>
    val projectPath = ws.runShell(ws.config[Seq[String]]("projectSetupCommand")).out.trim

    ws.run { intelliJ =>
      val projectName = intelliJ.probe.openProject(Paths.get(projectPath))
      // In headless mode closing project triggers saving settings that fails
      // For non headless mode it is enough to inspect notifications as saving would happen also after loading project
      intelliJ.probe.closeProject(projectName)

      val messages = intelliJ.probe.messages
      assertExists(messages) { msg =>
        msg.content.contains("Failed to save settings") || msg.content.contains("Save settings failed")
      }
    }
  }

}
