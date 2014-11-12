// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.oss;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

public class OSSPantsScalaExamplesIntegrationTest extends OSSPantsIntegrationTest {
  private static final String PLUGINS_KEY = "idea.load.plugins.id";
  private String defaultPlugins = null;

  @Override
  public void setUp() throws Exception {
    final String scalaPluginId = "org.intellij.scala";
    defaultPlugins = System.getProperty(PLUGINS_KEY);
    System.setProperty(PLUGINS_KEY, scalaPluginId + "," + defaultPlugins);

    super.setUp();

    final IdeaPluginDescriptor scalaPlugin = PluginManager.getPlugin(PluginId.getId(scalaPluginId));
    assertNotNull("Scala plugin should be in classpath for tests", scalaPlugin);
    if (!scalaPlugin.isEnabled()) {
      PluginManagerCore.enablePlugin(scalaPluginId);
    }
  }

  @Override
  public void tearDown() throws Exception {
    if (defaultPlugins != null) {
      System.setProperty(PLUGINS_KEY, defaultPlugins);
      defaultPlugins = null;
    }
    super.tearDown();
  }

  public void testHello() throws Throwable {
    doImport("examples/src/scala/com/pants/example/hello/exe");

    assertModules(
      "examples_src_resources_com_pants_example_hello_hello",
      "examples_src_scala_com_pants_example_hello_welcome_welcome",
      "examples_src_java_com_pants_examples_hello_greet_greet",
      "examples_src_scala_com_pants_example_hello_exe_exe"
    );

    makeModules("examples_src_scala_com_pants_example_hello_exe_exe");
    assertNotNull(
      findClassFile("com.pants.example.hello.exe.Exe", "examples_src_scala_com_pants_example_hello_exe_exe")
    );
  }
}
