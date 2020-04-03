package com.twitter.ideprobe

import com.twitter.ideprobe.dependencies.BundledDependencies

object ProbeTestPlugin {
  val id = "com.twitter.ideprobe.driver.test"
  val bundled = BundledDependencies.fromResources("driver-test-plugin")
}
