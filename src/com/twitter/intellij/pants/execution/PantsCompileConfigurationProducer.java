package com.twitter.intellij.pants.execution;

/**
 * Created by ajohnson on 6/12/14.
 */
public class PantsCompileConfigurationProducer extends PantsConfigurationProducerBase {
  public PantsCompileConfigurationProducer() {
    super("compile", "goal compile");
  }
}
