package com.twitter.intellij.pants

import org.virtuslab.ideprobe.IntegrationTestSuite
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.dependencies.Plugin

import scala.language.implicitConversions

class PantsTestSuite extends IntegrationTestSuite with OpenProjectFixture {

  val pantsProbePlugin: Plugin = Plugin.Bundled(s"ideprobe-pants-${BuildInfo.version}.zip")

  override def transformFixture(fixture: IntelliJFixture): IntelliJFixture = {
    fixture
      .copy(plugins = pantsProbePlugin +: fixture.plugins)
      .withAfterWorkspaceSetup(PantsSetup.overridePantsVersion)
  }

  implicit def pantsProbeDriver(driver: ProbeDriver): PantsProbeDriver = PantsProbeDriver(driver)
}
