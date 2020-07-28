package com.twitter.intellij.pants

import org.junit.Test
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IntelliJFixture

class PreparePants extends PantsTestSuite {
  @Test def run(): Unit = {
    val fixture = IntelliJFixture.fromConfig(Config.fromClasspath("pants.conf"))
    val workspace = fixture.setupWorkspace()
    workspace.resolve("pants").write("dummy").makeExecutable()
    PantsSetup.overridePantsVersion(fixture, workspace)
    runPants(workspace, Seq("goals"))
    runFastpass(fixture.config, workspace, Seq("--version"))
    fixture.deleteWorkspace(workspace)
  }
}
