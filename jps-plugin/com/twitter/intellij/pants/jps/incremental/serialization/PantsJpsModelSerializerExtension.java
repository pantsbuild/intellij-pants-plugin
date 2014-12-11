// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.serialization;

import com.twitter.intellij.pants.jps.incremental.model.impl.JpsPantsModuleExtensionImpl;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsModuleExtension;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

import java.util.Collections;
import java.util.List;

public class PantsJpsModelSerializerExtension extends JpsModelSerializerExtension {
  // same as in ExternalSystemConstants but it's be in an external process so we can't use it directly
  @NonNls @NotNull public static final String EXTERNAL_SYSTEM_ID_KEY  = "external.system.id";
  @NonNls @NotNull public static final String LINKED_PROJECT_ID_KEY   = "external.linked.project.id";
  @NonNls @NotNull public static final String LINKED_PROJECT_PATH_KEY = "external.linked.project.path";

  @Nullable
  public static JpsPantsModuleExtension findPantsModuleExtension(@NotNull JpsModule module) {
    return module.getContainer().getChild(JpsPantsModuleExtension.ROLE);
  }

  @NotNull
  @Override
  public List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Collections.singletonList(new PantsJpsProjectExtensionSerializer());
  }

  @Override
  public void loadModuleOptions(@NotNull JpsModule module, @NotNull Element rootElement) {
    super.loadModuleOptions(module, rootElement);
    final String externalSystemId = rootElement.getAttributeValue(EXTERNAL_SYSTEM_ID_KEY);
    final String linkedProjectId = rootElement.getAttributeValue(LINKED_PROJECT_ID_KEY);
    final String linkedProjectPath = rootElement.getAttributeValue(LINKED_PROJECT_PATH_KEY);
    if (PantsConstants.PANTS.equals(externalSystemId) && linkedProjectId != null && linkedProjectPath != null) {
      final JpsPantsModuleExtensionImpl moduleExtensionElement = new JpsPantsModuleExtensionImpl(linkedProjectPath, linkedProjectId);
      module.getContainer().setChild(JpsPantsModuleExtension.ROLE, moduleExtensionElement);
    }
  }
}
