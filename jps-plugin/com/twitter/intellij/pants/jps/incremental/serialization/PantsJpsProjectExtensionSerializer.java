// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.serialization;

import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsProjectExtension;
import com.twitter.intellij.pants.jps.incremental.model.impl.JpsPantsProjectExtensionImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

import java.util.Collections;
import java.util.List;

public class PantsJpsProjectExtensionSerializer extends JpsProjectExtensionSerializer {

  private static final String COMPILE_WITH_INTELLIJ   = "compileWithIntellij";
  private static final String LINKED_PROJECT_SETTINGS = "linkedExternalProjectsSettings";
  private static final String EXTERNAL_PROJECT_PATH   = "externalProjectPath";
  private static final String TARGETS                 = "targets";
  private static final String PROJECT_SETTINGS        = "PantsProjectSettings";

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
    if (projectPath == null) {
      return;
    }
    final boolean compileWithIntellij =
      Boolean.valueOf(JDOMExternalizerUtil.readField(componentTag, COMPILE_WITH_INTELLIJ, "false"));

    final Element targetsList = JDOMExternalizerUtil.getOption(projectSettings, TARGETS);
    final Element listOfTargetNames = targetsList != null ? targetsList.getChild("list") : null;
    final List<Element> targetNameOptions = listOfTargetNames != null ? JDOMUtil.getChildren(listOfTargetNames, "option") : Collections.<Element>emptyList();
    final JpsPantsProjectExtension projectExtension = new JpsPantsProjectExtensionImpl(
      projectPath,
      ContainerUtil.mapNotNull(
        targetNameOptions,
        new Function<Element, String>() {
          @Override
          public String fun(Element element) {
            return element.getAttributeValue("value");
          }
        }
      ),
      compileWithIntellij
    );

    project.getContainer().setChild(JpsPantsProjectExtension.ROLE, projectExtension);
  }

  @Override
  public void saveExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
    // do nothing
  }
}
