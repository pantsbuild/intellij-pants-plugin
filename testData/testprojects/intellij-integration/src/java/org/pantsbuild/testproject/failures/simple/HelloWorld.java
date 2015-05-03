// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

import java.lang.Deprecated;

/**
 * ./pants compile testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple:simple
 */
public class HelloWorld {
  @Deprecated
  public static deprecatedMethod() {
    /* bad code to generate warnings and errors */
    int unusedVar;
    if (1 == 2) {
    /* missed bracket: } */
  }
  public static void main(String[] args) {
    System.out.println("Hello World!") /* missed semicolon: ; */
  }
}
