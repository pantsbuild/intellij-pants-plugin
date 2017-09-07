// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;


public class PantsConstants {
  public static final String PANTS = "pants";
  public static final String PANTS_CONSOLE_NAME = "Pants Console";
  public static final String PLUGIN_ID = "com.intellij.plugins.pants";

  @NotNull
  public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId(PANTS);
  public static final String PANTS_PROJECT_MODULE_SUFFIX = "_module";
  public static final String PANTS_PROJECT_MODULE_ID_PREFIX = ".root.module.for.pants_";

  public static final String PANTS_INI = "pants.ini";
  public static final String PANTS_PEX = "pants.pex";
  public static final String PANTS_LIBRARY_NAME = PANTS_PEX;

  protected static final String BUILD = "BUILD";
  protected static final String THRIFT_EXT = "thrift";
  protected static final String ANTLR_EXT = "g";
  protected static final String ANTLR_4_EXT = "g4";
  protected static final String PROTOBUF_EXT = "proto";

  public static final String PANTS_LIBRARY_EXCLUDES_KEY = "pants.library.excludes";
  public static final String PANTS_TARGET_ADDRESSES_KEY = "pants.target.addresses";
  public static final String PANTS_TARGET_ADDRESS_INFOS_KEY = "pants.target.address.infos";

  public static final String PANTS_OPTION_PANTS_WORKDIR = "pants_workdir";
  public static final String PANTS_OPTION_TEST_JUNIT_STRICT_JVM_VERSION = "test.junit.strict_jvm_version";
  public static final String PANTS_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR = "export-classpath.manifest_jar_only";
  public static final String PANTS_OPTION_ASYNC_CLEAN_ALL = "clean-all.async";

  // Used to initialize project sdk therefore use project processing weight, i.e, the highest.
  public static final Key<Sdk> SDK_KEY = Key.create(Sdk.class, ProjectKeys.PROJECT.getProcessingWeight());

  public static final String PANTS_CLI_OPTION_EXPORT_OUTPUT_FILE = "--export-output-file";
  public static final String PANTS_CLI_OPTION_LIST_OUTPUT_FILE = "--list-output-file";
  public static final String PANTS_CLI_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR = "--export-classpath-manifest-jar-only";
  public static final String PANTS_CLI_OPTION_NO_COLORS = "--no-colors";
  public static final String PANTS_CLI_OPTION_JVM_DISTRIBUTIONS_PATHS = "--jvm-distributions-paths";
  public static final String PANTS_CLI_OPTION_NO_TEST_JUNIT_TIMEOUTS = "--no-test-junit-timeouts";
  public static final String PANTS_CLI_OPTION_ASYNC_CLEAN_ALL = "--async";
  public static final String PANTS_CLI_OPTION_PYTEST = "--test-pytest-options";
  public static final String PANTS_CLI_OPTION_JUNIT_TEST = "--test-junit-test";

  public static final String PANTS_EXPORT_KEY_STRICT = "strict";
  public static final String PANTS_EXPORT_KEY_NON_STRICT = "non_strict";

  public static final String ACTION_MAKE_PROJECT_ID = "CompileDirty";
  public static final String ACTION_MAKE_PROJECT_DESCRIPTION = "Make Project";
  public static final String REBUILD_PROJECT_DESCRIPTION = "Rebuild Project";
  public static final String ACTION_COMPILE_GROUP_ID = "ProjectViewCompileGroup";

  public static final String EXTERNAL_BUILDER_ERROR = "This is a Pants project. Please use PantsCompile under `Edit Configuration`";

  public static final String NOOP_COMPILE = "No changes in projects. Noop compile.";
}
