// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.util.text.CaseInsensitiveStringHashingStrategy;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;


public class PantsConstants {
  public static final String PANTS = "pants";
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

  public static final String PANTS_EXPORT_CLASSPATH_NAMING_STYLE_OPTION = "export-classpath.use_old_naming_style";
  public static final String PANTS_JVM_DISTRIBUTIONS_PATHS_OPTION = "--jvm-distributions-paths";

  public static Set<String> SUPPORTED_TARGET_TYPES = new THashSet<String>(
    Arrays.asList(
      "android_binary", "android_resources", "annotation_processor", "artifact", "artifactory", "bundle", "consume_thrift_libraries",
      "dependencies", "generated_resources", "hadoop_binary", "heron_binary", "jar", "jar_library", "java_agent", "java_antlr_library",
      "java_library", "java_protobuf_library", "java_ragel_library", "java_tests", "java_thrift_library",
      "java_thriftstore_dml_library", "java_wire_library", "jaxb_library", "junit_tests", "jvm_app", "jvm_binary", "resources",
      "scala_artifact", "scala_jar", "scala_library", "scala_specs", "scala_tests", "scalac_plugin", "storm_binary", "target",
      "thrift_jar", "python_binary", "python_library", "python_test_suite", "python_tests"
      ),
    CaseInsensitiveStringHashingStrategy.INSTANCE
  );
}
