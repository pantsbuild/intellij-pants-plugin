package com.twitter.intellij.pants

import com.twitter.ideprobe.Assertions
import com.twitter.ideprobe.IntegrationTestSuite
import com.twitter.ideprobe.IntelliJFixture
import org.junit.Assert
import org.junit.Test
import scala.util.control.NonFatal

trait BugfixTest extends IntegrationTestSuite with Assertions {

  def reproduce(fixture: IntelliJFixture): Unit

  @Test def reproduction(): Unit = reproduce(fixtureFromConfig("reproduction"))

  @Test def regression(): Unit = {
    try {
      reproduce(fixtureFromConfig("regression"))
      Assert.fail("Managed to reproduce the bug in regression test")
    } catch {
      case NonFatal(_) => // expected
    }
  }
}
