// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.util.containers.ContainerUtilRt;
import com.twitter.intellij.pants.model.PantsCompileOptions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class PantsProjectSettings extends ExternalProjectSettings implements PantsCompileOptions {
  private List<String> targetSpecs = ContainerUtilRt.newArrayList();
  public boolean libsWithSources;
  public boolean enableIncrementalImport;
  public boolean useIdeaProjectJdk;
  public boolean importSourceDepsAsJars;
  public boolean useIntellijCompiler;

  /**
   * @param targetSpecs               targets explicted listed from `pants idea-plugin` goal.
   * @param externalProjectPath       path to the Pants project.
   * @param libsWithSources           whether to import sources and docs when resolving for jars.
   * @param isEnableIncrementalImport whether to enabled incremental import.
   * @param isUseIdeaProjectJdk       whether use IDEA project JDK to compile Pants project.
   * @param isImportSourceDepsAsJars  whether to import source dependencies as frozen jars.
   * @param isUseIntellijCompiler     whether to use the IntelliJ compiler to compile the project (as opposed to using pants).
   */
  public PantsProjectSettings(
    List<String> targetSpecs,
    String externalProjectPath,
    boolean libsWithSources,
    boolean isEnableIncrementalImport,
    boolean isUseIdeaProjectJdk,
    boolean isImportSourceDepsAsJars,
    boolean isUseIntellijCompiler
  ) {
    setExternalProjectPath(externalProjectPath);
    this.targetSpecs = targetSpecs;
    this.libsWithSources = libsWithSources;
    enableIncrementalImport = isEnableIncrementalImport;
    useIdeaProjectJdk = isUseIdeaProjectJdk;
    importSourceDepsAsJars = isImportSourceDepsAsJars;
    useIdeaProjectJdk = isUseIntellijCompiler;
  }

  public PantsProjectSettings() {
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }
    PantsProjectSettings other = (PantsProjectSettings) obj;
    return Objects.equals(libsWithSources, other.libsWithSources)
           && Objects.equals(enableIncrementalImport, other.enableIncrementalImport)
           && Objects.equals(targetSpecs, other.targetSpecs)
           && Objects.equals(useIdeaProjectJdk, other.useIdeaProjectJdk)
           && Objects.equals(importSourceDepsAsJars, other.importSourceDepsAsJars)
           && Objects.equals(useIntellijCompiler, other.useIntellijCompiler);
  }

  @NotNull
  @Override
  public PantsProjectSettings clone() {
    final PantsProjectSettings pantsProjectSettings = new PantsProjectSettings();
    copyTo(pantsProjectSettings);
    return pantsProjectSettings;
  }

  @Override
  protected void copyTo(@NotNull ExternalProjectSettings receiver) {
    super.copyTo(receiver);
    if (receiver instanceof PantsProjectSettings) {
      ((PantsProjectSettings) receiver).libsWithSources = libsWithSources;
      ((PantsProjectSettings) receiver).setTargetSpecs(getTargetSpecs());
      ((PantsProjectSettings) receiver).enableIncrementalImport = enableIncrementalImport;
      ((PantsProjectSettings) receiver).useIdeaProjectJdk = useIdeaProjectJdk;
      ((PantsProjectSettings) receiver).importSourceDepsAsJars = importSourceDepsAsJars;
      ((PantsProjectSettings) receiver).useIntellijCompiler = useIntellijCompiler;
    }
  }


  /**
   * Get the target specs used to launched `pants idea-plugin`.
   */
  @NotNull
  public List<String> getTargetSpecs() {
    return targetSpecs;
  }

  public void setTargetSpecs(List<String> targetSpecs) {
    this.targetSpecs = targetSpecs;
  }

  @Override
  public boolean isEnableIncrementalImport() {
    return enableIncrementalImport;
  }

  @Override
  public boolean isImportSourceDepsAsJars() {
    return importSourceDepsAsJars;
  }

  public boolean isImportSourceDepsAsJars() {
    return myImportSourceDepsAsJars;
  }

  public void setImportSourceDepsAsJars(boolean importSourceDepsAsJars) {
    myImportSourceDepsAsJars = importSourceDepsAsJars;
  }

  public boolean isUseIntellijCompiler() {
    return myUseIntellijCompiler;
  }

  public void setUseIntellijCompiler(boolean useIntellijCompiler) {
    myUseIntellijCompiler = useIntellijCompiler;
  }
}
