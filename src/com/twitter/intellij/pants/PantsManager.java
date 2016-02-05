// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.PantsSystemProjectResolver;
import com.twitter.intellij.pants.service.task.PantsTaskManager;
import com.twitter.intellij.pants.settings.*;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.ExternalSystemIcons;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class PantsManager implements
                          ExternalSystemConfigurableAware,
                          ExternalSystemUiAware,
                          StartupActivity,
                          ExternalSystemManager<
                            PantsProjectSettings,
                            PantsSettingsListener,
                            PantsSettings,
                            PantsLocalSettings,
                            PantsExecutionSettings> {

  public static final FileChooserDescriptor BUILD_FILE_CHOOSER_DESCRIPTOR =
    new FileChooserDescriptor(true, true, false, false, false, false) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        return super.isFileSelectable(file) && PantsUtil.isPantsProjectFile(file);
      }
    };

  private static final Logger LOG = Logger.getInstance(PantsManager.class);

  @NotNull
  @Override
  public ProjectSystemId getSystemId() {
    return PantsConstants.SYSTEM_ID;
  }

  @Nullable
  @Override
  public Icon getProjectIcon() {
    return PantsIcons.Icon;
  }

  @Nullable
  @Override
  public Icon getTaskIcon() {
    return ExternalSystemIcons.Task;
  }

  @NotNull
  @Override
  public Configurable getConfigurable(@NotNull Project project) {
    return new PantsConfigurable(project);
  }


  @NotNull
  @Override
  public Function<Project, PantsSettings> getSettingsProvider() {
    return new Function<Project, PantsSettings>() {
      @Override
      public PantsSettings fun(Project project) {
        return PantsSettings.getInstance(project);
      }
    };
  }

  @NotNull
  @Override
  public Function<Project, PantsLocalSettings> getLocalSettingsProvider() {
    return new Function<Project, PantsLocalSettings>() {
      @Override
      public PantsLocalSettings fun(Project project) {
        return PantsLocalSettings.getInstance(project);
      }
    };
  }

  @NotNull
  @Override
  public Function<Pair<Project, String>, PantsExecutionSettings> getExecutionSettingsProvider() {
    return new Function<Pair<Project, String>, PantsExecutionSettings>() {
      @Override
      public PantsExecutionSettings fun(Pair<Project, String> projectStringPair) {
        final Project ideProject = projectStringPair.getFirst();
        final String projectPath = projectStringPair.getSecond();

        final PantsExecutionSettings executionSettings = getExecutionsSettingsFromPath(ideProject, projectPath);
        return executionSettings;
      }

      @NotNull
      public PantsExecutionSettings getExecutionsSettingsFromPath(@NotNull Project ideProject, @NotNull String projectPath) {
        boolean isUseIdeaProjectJdk = PantsSettings.getInstance(ideProject).isUseIdeaProjectJdk();

        final PantsTargetAddress absoluteTargetAddress = PantsTargetAddress.fromString(projectPath, true);

        if (absoluteTargetAddress != null) {
          return new PantsExecutionSettings(
            Collections.singletonList(absoluteTargetAddress.getTargetName()), false, true, isUseIdeaProjectJdk
          );
        }

        final AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(ideProject, PantsConstants.SYSTEM_ID);
        final ExternalProjectSettings projectSettings = systemSettings.getLinkedProjectSettings(projectPath);

        final List<String> targets = projectSettings instanceof PantsProjectSettings ?
                                     ((PantsProjectSettings)projectSettings).getTargetNames() : Collections.<String>emptyList();
        final boolean withDependees = projectSettings instanceof PantsProjectSettings &&
                                      ((PantsProjectSettings)projectSettings).isWithDependees();
        final boolean libsWithSources = projectSettings instanceof PantsProjectSettings &&
                                        ((PantsProjectSettings)projectSettings).isLibsWithSources();
        return new PantsExecutionSettings(targets, withDependees, libsWithSources, isUseIdeaProjectJdk);
      }
    };
  }

  @NotNull
  @Override
  public Class<? extends ExternalSystemProjectResolver<PantsExecutionSettings>> getProjectResolverClass() {
    return PantsSystemProjectResolver.class;
  }

  @Override
  public Class<? extends ExternalSystemTaskManager<PantsExecutionSettings>> getTaskManagerClass() {
    return PantsTaskManager.class;
  }

  @NotNull
  @Override
  public FileChooserDescriptor getExternalProjectDescriptor() {
    return BUILD_FILE_CHOOSER_DESCRIPTOR;
  }

  @NotNull
  @Override
  public String getProjectRepresentationName(@NotNull String targetProjectPath, @Nullable String rootProjectPath) {
    final PantsTargetAddress address = PantsTargetAddress.fromString(targetProjectPath, false);
    assert address != null;
    return address.getRelativePath() + ":" + address.getTargetName();
  }

  @Nullable
  @Override
  public FileChooserDescriptor getExternalProjectConfigDescriptor() {
    return BUILD_FILE_CHOOSER_DESCRIPTOR;
  }

  @Override
  public void enhanceRemoteProcessing(@NotNull SimpleJavaParameters parameters) throws ExecutionException {
    parameters.getVMParametersList().addProperty(
      ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, PantsConstants.SYSTEM_ID.getId()
    );
  }

  @Override
  public void enhanceLocalProcessing(@NotNull List<URL> urls) {
  }

  @Override
  public void runActivity(@NotNull Project project) {
  }
}
