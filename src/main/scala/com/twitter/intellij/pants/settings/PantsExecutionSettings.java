// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;

import com.twitter.intellij.pants.model.PantsExecutionOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PantsExecutionSettings extends ExternalSystemExecutionSettings implements PantsExecutionOptions {
  private final String myName;
  private final boolean myLibsWithSourcesAndDocs;
  private final boolean myUseIdeaProjectJdk;
  private final boolean myUseIntellijCompiler;
  private final Optional<Integer> myIncrementalImportDepth;

  private final boolean myImportSourceDepsAsJars;
  private final List<String> myTargetSpecs;

  private static final String DEFAULT_PROJECT_NAME = null;
  private static final List<String> DEFAULT_TARGET_SPECS = Collections.emptyList();
  private static final boolean DEFAULT_WITH_SOURCES_AND_DOCS = true;
  private static final boolean DEFAULT_USE_IDEA_PROJECT_SDK = false;
  private static final Optional<Integer> DEFAULT_INCREMENTAL_IMPORT = Optional.empty();
  private static final boolean DEFAULT_IMPORT_SOURCE_DEPS_AS_JARS = false;
  private static final boolean DEFAULT_USE_INTELLIJ_COMPILER = false;

  public static PantsExecutionSettings createDefault() {
    return new PantsExecutionSettings(
      DEFAULT_PROJECT_NAME,
      DEFAULT_TARGET_SPECS,
      DEFAULT_WITH_SOURCES_AND_DOCS,
      DEFAULT_USE_IDEA_PROJECT_SDK,
      DEFAULT_IMPORT_SOURCE_DEPS_AS_JARS,
      DEFAULT_INCREMENTAL_IMPORT,
      DEFAULT_USE_INTELLIJ_COMPILER
    );
  }

  public PantsExecutionSettings(
    String name,
    List<String> targetSpecs,
    boolean libsWithSourcesAndDocs,
    boolean useIdeaProjectJdk,
    boolean importSourceDepsAsJars,
    Optional<Integer> enableIncrementalImport,
    boolean useIntellijCompiler
  ){
    myName = name;
    myTargetSpecs = targetSpecs;
    myLibsWithSourcesAndDocs = libsWithSourcesAndDocs;
    myUseIdeaProjectJdk = useIdeaProjectJdk;
    myImportSourceDepsAsJars = importSourceDepsAsJars;
    myIncrementalImportDepth = enableIncrementalImport;
    myUseIntellijCompiler = useIntellijCompiler;
  }

  /**
   * @param targetSpecs             targets explicitly listed from `pants idea-plugin` goal.
   * @param libsWithSourcesAndDocs  whether to import sources and docs when resolving for jars.
   * @param useIdeaProjectJdk       whether to explicitly use the JDK selected in project for Pants compile.
   * @param enableIncrementalImport whether to incrementally import the project.
   */
  public PantsExecutionSettings(
    List<String> targetSpecs,
    boolean libsWithSourcesAndDocs,
    boolean useIdeaProjectJdk,
    boolean importSourceDepsAsJars,
    Optional<Integer> enableIncrementalImport,
    boolean useIntellijCompiler
  ) {
    this(DEFAULT_PROJECT_NAME, targetSpecs, libsWithSourcesAndDocs, useIdeaProjectJdk, importSourceDepsAsJars, enableIncrementalImport, useIntellijCompiler);
  }

  public Optional<String> getProjectName(){
    return Optional.ofNullable(myName)
      .filter(name -> !name.isEmpty());
  }

  @NotNull
  public List<String> getSelectedTargetSpecs() {
    return myTargetSpecs;
  }

  public boolean isLibsWithSourcesAndDocs() {
    return myLibsWithSourcesAndDocs;
  }

  public boolean isUseIdeaProjectJdk() {
    return myUseIdeaProjectJdk;
  }

  public Optional<Integer> incrementalImportDepth() {
    return myIncrementalImportDepth;
  }

  @Override
  public boolean isImportSourceDepsAsJars() {
    return myImportSourceDepsAsJars;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    PantsExecutionSettings settings = (PantsExecutionSettings) o;
    return Objects.equals(myUseIdeaProjectJdk, settings.myUseIdeaProjectJdk) &&
           Objects.equals(myIncrementalImportDepth, settings.myIncrementalImportDepth) &&
           Objects.equals(myUseIntellijCompiler, settings.myUseIntellijCompiler) &&
           Objects.equals(myLibsWithSourcesAndDocs, settings.myLibsWithSourcesAndDocs) &&
           Objects.equals(myTargetSpecs, settings.myTargetSpecs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      myTargetSpecs,
      myLibsWithSourcesAndDocs,
      myUseIdeaProjectJdk,
      myIncrementalImportDepth,
      myUseIntellijCompiler
    );
  }
}
