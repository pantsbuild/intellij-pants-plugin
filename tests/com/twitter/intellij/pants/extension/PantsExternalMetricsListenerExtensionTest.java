// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.extension;

import com.intellij.ExtensionPoints;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListener;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListenerManager;
import com.twitter.intellij.pants.util.PantsConstants;
import org.picocontainer.defaults.DefaultPicoContainer;

import java.util.ArrayList;
import java.util.List;

public class PantsExternalMetricsListenerExtensionTest extends LightCodeInsightFixtureTestCase {

  public class DummyMetricsListener implements PantsExternalMetricsListener{

    @Override
    public void logIncrementalImport(boolean isIncremental) {

    }

    @Override
    public void logGUIImport(boolean isGUI) {

    }

    @Override
    public void logTestRunner(TestRunnerType runner) {
      System.out.println("hehe");

    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Extensions.getRootArea().getExtensionPoint(PantsExternalMetricsListener.EP_NAME).registerExtension(new DummyMetricsListener());
  }

  public void testLogTestRunner() {
    //PantsExternalMetricsListener[] extensions = PantsExternalMetricsListener.EP_NAME.getExtensions();
    //System.out.println(extensions);
    //System.out.println(extensions.length);
    //int x = 5;
    PantsExternalMetricsListenerManager.getInstance().logTestRunner(PantsExternalMetricsListener.TestRunnerType.JUNIT_RUNNER);
  }
}
