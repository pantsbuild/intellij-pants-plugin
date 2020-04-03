package com.twitter.ideprobe

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(classOf[JUnit4])
final class ConfigurationTest {
  @Test
  def resolvesProperty(): Unit = {
    verifyResolution(property = "${a}", expected = "A")
  }

  @Test
  def resolvesSuffixProperty(): Unit = {
    verifyResolution(property = "foo${a}", expected = "fooA")
  }

  @Test
  def resolvesPrefixProperty(): Unit = {
    verifyResolution(property = "${a}foo", expected = "Afoo")
  }

  @Test
  def resolvesConsecutiveProperties(): Unit = {
    verifyResolution(property = "${a}${b}", expected = "AB")
  }

  @Test
  def resolvesSurroundingProperties(): Unit = {
    verifyResolution(property = "${a}foo${b}", expected = "AfooB")
  }

  private def verifyResolution(property: String, expected: String): Unit = {
    val propertyName = "property"
    val configuration = Config.fromString(s"""
        |a=A
        |b=B
        |$propertyName=$property
        |""".stripMargin)

    configuration.get[String](propertyName) match {
      case Some(actual) =>
        Assert.assertEquals(expected, actual)
      case None =>
        Assert.fail("Failed to resolve config")
    }
  }
}
