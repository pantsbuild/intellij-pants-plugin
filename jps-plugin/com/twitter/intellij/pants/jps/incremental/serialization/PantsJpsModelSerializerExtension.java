// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

import java.util.Collections;
import java.util.List;

public class PantsJpsModelSerializerExtension extends JpsModelSerializerExtension {
  @NotNull
  @Override
  public List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Collections.singletonList(new PantsJpsProjectExtensionSerializer());
  }
}
