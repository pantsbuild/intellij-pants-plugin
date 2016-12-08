// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.twitter.intellij.pants.model.PantsExecutionOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PantsExecutionSettings extends ExternalSystemExecutionSettings implements PantsExecutionOptions {
  private final boolean myWithDependees;
  private final boolean myLibsWithSourcesAndDocs;
  private final boolean myUseIdeaProjectJdk;
  private final boolean myEnableIncrementalImport;
  private final List<String> myTargetSpecs;

  private static final List<String> DEFAULT_TARGET_SPECS = Collections.emptyList();
  private static final boolean DEFAULT_WITH_DEPENDEES = false;
  private static final boolean DEFAULT_WITH_SOURCES_AND_DOCS = true;
  private static final boolean DEFAULT_USE_IDEA_PROJECT_SDK = false;
  private static final boolean DEFAULT_ENABLE_INCREMENTAL_IMPORT = false;

  public static PantsExecutionSettings createDefault() {
    return new PantsExecutionSettings(
      DEFAULT_TARGET_SPECS,
      DEFAULT_WITH_DEPENDEES,
      DEFAULT_WITH_SOURCES_AND_DOCS,
      DEFAULT_USE_IDEA_PROJECT_SDK,
      DEFAULT_ENABLE_INCREMENTAL_IMPORT
    );
  }

  /**
   * @param targetSpecs:             targets explicitly listed from `pants idea-plugin` goal.
   * @param withDependees:           whether depeedees need to be imported. (Untested and probably not working).
   * @param libsWithSourcesAndDocs:  whether to import sources and docs when resolving for jars.
   * @param useIdeaProjectJdk:       whether to explicitly use the JDK selected in project for Pants compile.
   * @param enableIncrementalImport: whether to incrementally import the project.
   */
  public PantsExecutionSettings(
    List<String> targetSpecs,
    boolean withDependees,
    boolean libsWithSourcesAndDocs,
    boolean useIdeaProjectJdk,
    boolean enableIncrementalImport
  ) {
    myTargetSpecs = targetSpecs;
    myWithDependees = withDependees;
    myLibsWithSourcesAndDocs = libsWithSourcesAndDocs;
    myUseIdeaProjectJdk = useIdeaProjectJdk;
    myEnableIncrementalImport = enableIncrementalImport;
  }

  @NotNull
  public List<String> getTargetSpecs() {
    return myTargetSpecs;
  }

  @Override
  public boolean isWithDependees() {
    return myWithDependees;
  }

  public boolean isLibsWithSourcesAndDocs() {
    return myLibsWithSourcesAndDocs;
  }

  public boolean isUseIdeaProjectJdk() {
    return myUseIdeaProjectJdk;
  }

  public boolean isEnableIncrementalImport() {
    return myEnableIncrementalImport;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    PantsExecutionSettings settings = (PantsExecutionSettings) o;
    if (myUseIdeaProjectJdk != settings.myUseIdeaProjectJdk) return false;
    if (myEnableIncrementalImport != settings.myEnableIncrementalImport) return false;
    if (myWithDependees != settings.myWithDependees) return false;
    if (myLibsWithSourcesAndDocs != settings.myLibsWithSourcesAndDocs) return false;
    if (myTargetSpecs != null ? !myTargetSpecs.equals(settings.myTargetSpecs) : settings.myTargetSpecs != null) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      myTargetSpecs,
      myWithDependees,
      myLibsWithSourcesAndDocs,
      myUseIdeaProjectJdk,
      myEnableIncrementalImport
    );
  }
}
