// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.google.common.collect.Sets;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.junit.AllInDirectoryConfigurationProducer;
import com.intellij.execution.junit.AllInPackageConfigurationProducer;
import com.intellij.execution.junit.TestInClassConfigurationProducer;

import java.util.Set;

public class DefaultRunConfigurationSelector {
  private final static Set<Class> JUNIT_CONFIGS = Sets.newHashSet(
    AllInPackageConfigurationProducer.class,
    AllInDirectoryConfigurationProducer.class,
    TestInClassConfigurationProducer.class
  );

  public enum DefaultTestRunner {
    ALL, JUNIT, PANTS
  }

  public static void registerConfigs(DefaultRunConfigurationSelector.DefaultTestRunner selectedItem) {
    switch (selectedItem) {
      case ALL:
        DefaultRunConfigurationSelector.enableIdeaJUnitConfigurations();
        DefaultRunConfigurationSelector.enableIdeaPantsConfigurations();
        break;
      case JUNIT:
        DefaultRunConfigurationSelector.enableIdeaJUnitConfigurations();
        DefaultRunConfigurationSelector.disableIdeaPantsConfigurations();
        break;
      case PANTS:
        DefaultRunConfigurationSelector.disableIdeaJUnitConfigurations();
        DefaultRunConfigurationSelector.enableIdeaPantsConfigurations();
        break;
      default:
        DefaultRunConfigurationSelector.enableIdeaJUnitConfigurations();
        DefaultRunConfigurationSelector.enableIdeaPantsConfigurations();
    }
  }

  public static void disableIdeaJUnitConfigurations() {
    RunConfigurationProducer.EP_NAME.getPoint(null).extensions()
      .filter(runConfig -> JUNIT_CONFIGS.contains(runConfig.getClass()))
      .forEach(config -> {
        RunConfigurationProducer.EP_NAME.getPoint(null).unregisterExtension(config);
      });
  }

  public static void enableIdeaJUnitConfigurations() {
    if (RunConfigurationProducer.EP_NAME.getPoint(null).extensions().noneMatch(config -> JUNIT_CONFIGS.contains(config.getClass()))) {
      JUNIT_CONFIGS.forEach(config -> {
        try {
          RunConfigurationProducer producer = (RunConfigurationProducer) config.getConstructor().newInstance();
          RunConfigurationProducer.EP_NAME.getPoint(null).registerExtension(producer);
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  public static void disableIdeaPantsConfigurations() {
    RunConfigurationProducer.EP_NAME.getPoint(null).extensions()
      .filter(runConfig -> runConfig instanceof PantsJUnitTestRunConfigurationProducer)
      .forEach(config -> RunConfigurationProducer.EP_NAME.getPoint(null).unregisterExtension(config));
  }

  public static void enableIdeaPantsConfigurations() {
    if (RunConfigurationProducer.EP_NAME.getPoint(null).extensions()
      .noneMatch(config -> config instanceof PantsJUnitTestRunConfigurationProducer)) {
      RunConfigurationProducer.EP_NAME.getPoint(null).registerExtension(new PantsJUnitTestRunConfigurationProducer());
    }
  }
}
