// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.pants.testproject.missingdepswhitelist

import com.pants.testproject.publish.hello.greet.Greeting
import com.pants.testproject.missingdepswhitelist2.MissingDepsWhitelist2

object MissingDepsWhitelist {
  def doStuff() = {
    val temp = new MissingDepsWhitelist2()
    Greeting.greet("woop")
  }
}