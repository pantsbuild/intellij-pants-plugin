package com.example

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.MustMatchers

@RunWith(classOf[JUnitRunner])
class WelSpec extends WordSpec with MustMatchers {
  "Welcome" should {
    "greet nobody" in {
      greet(List()).size mustEqual 0
    }
    "greet both" in {
      greet(List("Pat", "Sandy")).size mustEqual 2
    }
  }

  private def greet(list: List[String]): List[String] = list.map("Hi " + _)
}
