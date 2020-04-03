package com.twitter.ideprobe.examples

import com.twitter.ideprobe.Assertions
import com.twitter.ideprobe.IntegrationTestSuite
import com.twitter.ideprobe.Assertions
import com.twitter.ideprobe.Extensions._
import com.twitter.ideprobe.IntegrationTestSuite
import com.twitter.ideprobe.IntelliJFixture
import com.twitter.ideprobe.dependencies.IntelliJVersion
import com.twitter.ideprobe.dependencies.Plugin
import org.junit.Assert._
import org.junit.Test

class ExampleTests extends IntegrationTestSuite with Assertions {

  val fixture = IntelliJFixture(
    version = IntelliJVersion.Latest,
    plugins = List(Plugin("org.intellij.scala", "2019.3.23"))
  )

  @Test def badScalaPluginVersion(): Unit = {
    fixture.run { intelliJ =>
      val errors = intelliJ.probe.errors
      assertExists(errors)(error =>
        error.content.contains("""plugin is incompatible""")
          && error.content.contains("The Scala (")
      )
    }
  }

  @Test def badScalaPluginVersionWithUninstall(): Unit = {
    fixture.withWorkspace { ws =>
      ws.run { intelliJ =>
        val errors = intelliJ.probe.errors
        assertExists(errors)(error =>
          error.content.contains("""plugin is incompatible""")
            && error.content.contains("The Scala (")
        )
      }

      ws.intelliJPaths.plugins.resolve("Scala").delete()

      ws.run { intelliJ =>
        val errors = intelliJ.probe.errors
        assertEquals(Nil, errors)
      }
    }
  }

}
