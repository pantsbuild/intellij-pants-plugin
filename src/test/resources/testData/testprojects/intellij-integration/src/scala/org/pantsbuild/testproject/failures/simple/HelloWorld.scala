// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package org.pantsbuild.testproject.failures.simple;

/**
 * ./pants compile.scala testData/testprojects/intellij-integration/src/scala/org/pantsbuild/testproject/failures/exe:exe
 */

object HelloWorld {
  /* bad code to generate errors */
  def badMethod() {
    if () /* missed brackets: {} */
  }

  def main(args: Array[String]) {
      println("Hello, world!")
    /* missed bracket: } */
}
