package com.twitter.ideprobe

import java.nio.file.Path
import com.twitter.ideprobe.dependencies.IntelliJVersion
import com.zaxxer.nuprocess.NuProcess
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.Description
import scala.concurrent.duration._

final class IntelliJRuleTest extends IntegrationTestSuite with WorkspaceFixture with Assertions {

  @Test // TODO add same test for runningIntelliJ.idePID when on java 9
  def shutdownsLauncherAfterTest(): Unit = {
    var process: NuProcess = null
    execute { rule =>
      process = rule.runningIde.launcher
    }

    within(5.seconds) {
      assertFalse("Process remains alive after test", process.isRunning)
    }
  }

  @Test
  def cleansRuleStateAfterTest(): Unit = {
    var rule: IntelliJRule = null
    execute { actualRule =>
      rule = actualRule
    }

    assertNull(rule.installedIntelliJ)
    assertNull(rule.runningIde)
  }

  private def execute(f: IntelliJRule => Unit): Unit = withWorkspace { workspace =>
    val intelliJ = freshIntelliJ(workspace)
    val statement = intelliJ.apply(() => f(intelliJ), Description.EMPTY)
    statement.evaluate()
  }

  private def freshIntelliJ(workspace: Path): IntelliJRule = {
    IntelliJFixture(WorkspaceTemplate.FromFile(workspace), IntelliJVersion.Latest, plugins = Nil).rule
  }

}
