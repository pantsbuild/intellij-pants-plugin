// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

public class InvalidTargetException extends Throwable {
  private final String myTargetString;
  private final String myMessage;

  InvalidTargetException(String targetString, String message) {
    myTargetString = targetString;
    myMessage = message;
  }

  @Override
  public String getMessage() {
    return "[" + myTargetString + "]: " + myMessage;
  }
}
