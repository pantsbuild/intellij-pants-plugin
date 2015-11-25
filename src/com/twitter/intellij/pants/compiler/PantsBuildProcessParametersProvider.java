// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler;

import com.intellij.compiler.server.BuildProcessParametersProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.jps.incremental.serialization.PantsJpsProjectExtensionSerializer;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;

import java.util.List;

public class PantsBuildProcessParametersProvider extends BuildProcessParametersProvider {
  @NotNull
  @Override
  public List<String> getClassPath() {
    // Hack to provide additional classpath to compilation process
    // in BuildManager. See scripts/prepare-ci-environment.sh
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final List<String> classpath = ContainerUtil.newArrayList();
      classpath.add(ClasspathBootstrap.getResourcePath(PantsJpsProjectExtensionSerializer.class));
      classpath.add(ClasspathBootstrap.getResourcePath(PantsUtil.class));
      classpath.addAll(StringUtil.split(StringUtil.notNullize(System.getProperty("pants.jps.plugin.classpath")), ":"));
      return classpath;
    }
    return super.getClassPath();
  }
}
