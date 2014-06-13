package com.twitter.intellij.pants.execution;

import com.intellij.execution.configurations.ConfigurationFactory;


/**
 * Created by ajohnson on 6/12/14.
 */
public class PantsRunConfigurationProducer extends PantsConfigurationProducerBase {

  public PantsRunConfigurationProducer() {
    super("run", "goal run ");
  }

}
