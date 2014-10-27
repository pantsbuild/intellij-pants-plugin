package com.twitter.intellij.pants.execution;

public class PantsRunConfigurationProducer extends PantsConfigurationProducerBase {

  public PantsRunConfigurationProducer() {
    super("run", "goal run");
  }
}
