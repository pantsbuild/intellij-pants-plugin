// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

/**
 * Created by ajohnson on 6/12/14.
 */
public class PantsCompileConfigurationProducer extends PantsConfigurationProducerBase {
  public PantsCompileConfigurationProducer() {
    super("compile", "goal compile");
  }
}
