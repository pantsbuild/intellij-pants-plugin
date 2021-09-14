// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package org.pantsbuild.testproject.missingdepswhitelist2

import org.pantsbuild.testproject.publish.hello.greet.Greeting

class MissingDepsWhitelist2 {
  def doStuff() = Greeting.greet("weep")
}