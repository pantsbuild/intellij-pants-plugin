// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.config;

import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PantsProjectCompilerConfigurable extends BaseConfigurable implements SearchableConfigurable {
  private final Project myProject;

  private PantsProjectCompilerForm myCompilerForm = new PantsProjectCompilerForm();

  public PantsProjectCompilerConfigurable(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public String getId() {
    return PantsConstants.PANTS;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return PantsBundle.message("pants.name");
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myCompilerForm.getMainPanel();
  }

  @Override
  public boolean isModified() {
    return PantsSettings.getInstance(myProject).isCompileWithIntellij() != myCompilerForm.isCompileWithIntellij();
  }

  @Override
  public void apply() throws ConfigurationException {
    final PantsSettings pantsSettings = PantsSettings.getInstance(myProject);
    final boolean refreshNeeded = pantsSettings.isCompileWithIntellij() != myCompilerForm.isCompileWithIntellij();
    pantsSettings.setCompileWithIntellij(myCompilerForm.isCompileWithIntellij());
    if (refreshNeeded) {
      PantsUtil.refreshAllProjects(myProject);
    }
  }

  @Override
  public void reset() {
    myCompilerForm.setCompileWithIntellij(PantsSettings.getInstance(myProject).isCompileWithIntellij());
  }

  @Override
  public void disposeUIResources() {
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myCompilerForm.getCompilerComboBox();
  }
}
