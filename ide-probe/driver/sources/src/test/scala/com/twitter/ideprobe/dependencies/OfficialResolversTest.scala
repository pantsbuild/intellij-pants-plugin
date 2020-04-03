package com.twitter.ideprobe.dependencies

import java.net.HttpURLConnection
import java.net.URI
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(classOf[JUnit4])
final class OfficialResolversTest {
  @Test
  def resolvesBuildToExistingArtifact(): Unit = {
    val version = IntelliJVersion.Latest
    val uri = IntelliJResolver.Official.resolve(version)

    verify(uri)
  }

  @Test
  def resolvesPluginToExistingArtifact(): Unit = {
    val plugin = Plugin("org.intellij.scala", "2020.1.10")
    val uri = PluginResolver.Official.resolve(plugin)

    verify(uri)
  }

  private def verify(uri: URI): Unit = {
    uri.toURL.openConnection() match {
      case connection: HttpURLConnection =>
        connection.setRequestMethod("GET")
        assertEquals(connection.getResponseCode, 200)
      case _ =>
        fail(s"Not a http connection to $uri")
    }
  }
}
