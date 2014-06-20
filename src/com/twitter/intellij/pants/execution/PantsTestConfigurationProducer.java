package com.twitter.intellij.pants.execution;

/**
 * Created by ajohnson on 6/12/14.
 */
public class PantsTestConfigurationProducer extends PantsConfigurationProducerBase {
  public PantsTestConfigurationProducer() {
    super("test","goal test");
  }
}
