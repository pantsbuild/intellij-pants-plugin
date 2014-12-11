// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import org.jetbrains.annotations.NotNull;


public class PantsConstants {
  public static final String PANTS = "pants";
  public static final String PLUGIN_ID = "com.intellij.plugins.pants";

  @NotNull
  public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId(PANTS);
  public static final String PANTS_PROJECT_MODULE_SUFFIX = "_module";

  public static final String PANTS_LIBRARY_NAME = "pants";

  public static final String PANTS_INI = "pants.ini";

  protected static final String BUILD = "BUILD";
  protected static final String THRIFT_EXT = "thrift";
  protected static final String ANTLR_EXT = "g";
  protected static final String ANTLR_4_EXT = "g4";
  protected static final String PROTOBUF_EXT = "proto";
}
