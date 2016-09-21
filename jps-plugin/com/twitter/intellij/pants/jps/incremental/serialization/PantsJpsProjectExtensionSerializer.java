// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.serialization;

import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.twitter.intellij.pants.PantsException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

public class PantsJpsProjectExtensionSerializer extends JpsProjectExtensionSerializer {

  private static final String LINKED_PROJECT_SETTINGS = "linkedExternalProjectsSettings";
  private static final String PROJECT_SETTINGS = "PantsProjectSettings";

  public PantsJpsProjectExtensionSerializer() {
    super("pants.xml", "PantsSettings");
  }

  @Override
  public void loadExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
    final Element linkedSettings = JDOMExternalizerUtil.getOption(componentTag, LINKED_PROJECT_SETTINGS);
    final Element projectSettings = linkedSettings != null ? linkedSettings.getChild(PROJECT_SETTINGS) : null;
    if (projectSettings != null) {
      throw new PantsException("Please use PantsCompile under `Edit Configuration`");
    }
  }

  @Override
  public void saveExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
    // do nothing
  }
}
