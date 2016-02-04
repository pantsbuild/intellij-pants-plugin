// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.serialization;

import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsProjectExtension;
import com.twitter.intellij.pants.jps.incremental.model.impl.JpsPantsProjectExtensionImpl;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

import java.io.File;

public class PantsJpsProjectExtensionSerializer extends JpsProjectExtensionSerializer {

  private static final String COMPILE_WITH_INTELLIJ = "compileWithIntellij";
  private static final String ENFORCE_JDK = "useIdeaProjectJdk";
  private static final String LINKED_PROJECT_SETTINGS = "linkedExternalProjectsSettings";
  private static final String EXTERNAL_PROJECT_PATH = "externalProjectPath";
  private static final String PROJECT_SETTINGS = "PantsProjectSettings";

  @Nullable
  public static JpsPantsProjectExtension findPantsProjectExtension(@NotNull JpsProject project) {
    return project.getContainer().getChild(JpsPantsProjectExtension.ROLE);
  }

  public PantsJpsProjectExtensionSerializer() {
    super("pants.xml", "PantsSettings");
  }

  @Override
  public void loadExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
    final Element linkedSettings = JDOMExternalizerUtil.getOption(componentTag, LINKED_PROJECT_SETTINGS);
    final Element projectSettings = linkedSettings != null ? linkedSettings.getChild(PROJECT_SETTINGS) : null;
    if (projectSettings == null) {
      return;
    }
    final String projectPath = JDOMExternalizerUtil.readField(projectSettings, EXTERNAL_PROJECT_PATH);
    final File pantsExecutable = projectPath != null ? PantsUtil.findPantsExecutable(new File(projectPath)) : null;
    if (pantsExecutable == null) {
      return;
    }
    final boolean compileWithIntellij =
      Boolean.valueOf(JDOMExternalizerUtil.readField(componentTag, COMPILE_WITH_INTELLIJ, "false"));
    final boolean useIdeaProjectJdk = Boolean.valueOf(JDOMExternalizerUtil.readField(componentTag, ENFORCE_JDK, "false"));

    final JpsPantsProjectExtension projectExtension =
      new JpsPantsProjectExtensionImpl(pantsExecutable.getPath(), compileWithIntellij, useIdeaProjectJdk);

    project.getContainer().setChild(JpsPantsProjectExtension.ROLE, projectExtension);
  }

  @Override
  public void saveExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
    // do nothing
  }
}
