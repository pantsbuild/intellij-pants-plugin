// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler;

import com.intellij.compiler.server.BuildProcessParametersProvider;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.jps.incremental.serialization.PantsJpsModelSerializerExtension;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.fest.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;

import java.util.List;

public class PantsBuildProcessParametersProvider extends BuildProcessParametersProvider {

  private final Project myProject;

  public PantsBuildProcessParametersProvider(Project project) {
    myProject = project;
  }


  @NotNull
  @Override
  public List<String> getClassPath() {
    // Hack to provide additional classpath to compilation process
    // in BuildManager. See scripts/prepare-ci-environment.sh

    if (PantsUtil.isPantsProject(myProject)) {
      throw new RuntimeException(PantsConstants.EXTERNAL_BUILDER_ERROR);
    }

    //// IDEA 2016 moved `ExternalSystemException` out of `openapi.jar` and is not on the default classpaths anymore
    //// despite staying in the same package, so we have to explicitly add it to the external builder.
    //final List<String> classpath = ContainerUtil.newArrayList();
    //classpath.add(ClasspathBootstrap.getResourcePath(ExternalSystemException.class));
    //
    //final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(PantsConstants.PLUGIN_ID));
    //classpath.add(((IdeaPluginDescriptorImpl) plugin).getClassPath().toString());
    //
    ////if (ApplicationManager.getApplication().isUnitTestMode()) {
    //  classpath.add(ClasspathBootstrap.getResourcePath(PantsUtil.class));
    //  classpath.add(ClasspathBootstrap.getResourcePath(PantsJpsModelSerializerExtension.class));
    //  classpath.addAll(StringUtil.split(StringUtil.notNullize(System.getProperty("pants.jps.plugin.classpath")), ":"));
    ////}
    //
    return Lists.newArrayList();
  }
}
