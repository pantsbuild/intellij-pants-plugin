// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.serialization;

import com.google.gson.Gson;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsModuleExtension;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

import java.util.Collections;
import java.util.List;

public class PantsJpsModelSerializerExtension extends JpsModelSerializerExtension {
  // same as in ExternalSystemConstants but it is used in an external process so we can't use it directly
  @NonNls @NotNull public static final String EXTERNAL_SYSTEM_ID_KEY = "external.system.id";
  @NonNls @NotNull public static final String LINKED_PROJECT_PATH_KEY = "external.linked.project.path";
  public static final Gson gson = new Gson();

  @Nullable
  public static JpsPantsModuleExtension findPantsModuleExtension(@Nullable JpsModule module) {
    return module != null ? module.getContainer().getChild(JpsPantsModuleExtension.ROLE) : null;
  }

  @NotNull
  @Override
  public List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Collections.singletonList(new PantsJpsProjectExtensionSerializer());
  }
}
