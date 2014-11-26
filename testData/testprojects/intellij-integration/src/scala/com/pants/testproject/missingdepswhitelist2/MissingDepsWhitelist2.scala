package com.pants.testproject.missingdepswhitelist2

import com.pants.testproject.publish.hello.greet.Greeting

object MissingDepsWhitelist2 {
  def doStuff() = Greeting.greet("weep")
}