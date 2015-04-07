// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package org.pantsbuild.testproject.missingdepswhitelist

import org.pantsbuild.testproject.publish.hello.greet.Greeting
import org.pantsbuild.testproject.missingdepswhitelist2.MissingDepsWhitelist2

object MissingDepsWhitelist {
  def doStuff() = {
    val temp = new MissingDepsWhitelist2()
    Greeting.greet("woop")
  }
}