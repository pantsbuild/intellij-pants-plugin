package com.twitter.intellij.pants

import com.twitter.ideprobe.Config
import com.twitter.ideprobe.IntegrationTestSuite
import com.twitter.ideprobe.IntelliJFixture
import com.twitter.ideprobe.ProbeDriver
import com.twitter.ideprobe.dependencies.BundledDependencies
import com.twitter.ideprobe.dependencies.Plugin
import scala.language.implicitConversions

class PantsTestSuite extends IntegrationTestSuite {

  val pantsProbePlugin: Plugin = BundledDependencies.fromResources("ideprobe-pants")

  override def fixtureFromConfig(name: String): IntelliJFixture = {
    val fixture = super.fixtureFromConfig(name)
    fixture.copy(plugins = pantsProbePlugin +: fixture.plugins)
  }

  def fixtureFromConfig(config: Config): IntelliJFixture = {
    val fixture = IntelliJFixture.fromConfig(config)
    fixture.copy(plugins = pantsProbePlugin +: fixture.plugins)
  }

  implicit def pantsProbeDriver(driver: ProbeDriver): PantsProbeDriver = PantsProbeDriver(driver)
}
