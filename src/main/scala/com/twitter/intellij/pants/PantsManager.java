// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants;

import com.intellij.execution.configurations.SimpleJavaParameters;
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
import com.twitter.intellij.pants.service.project.PantsSystemProjectResolver;
import com.twitter.intellij.pants.service.task.PantsTaskManager;
import com.twitter.intellij.pants.settings.PantsConfigurable;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.settings.PantsLocalSettings;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.settings.PantsSettingsListener;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.ExternalSystemIcons;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Optional;

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
    return PantsSettings::getInstance;
  }

  @NotNull
  @Override
  public Function<Project, PantsLocalSettings> getLocalSettingsProvider() {
    return PantsLocalSettings::getInstance;
  }

  @NotNull
  @Override
  public Function<Pair<Project, String>, PantsExecutionSettings> getExecutionSettingsProvider() {
    return new Function<Pair<Project, String>, PantsExecutionSettings>() {
      @Override
      public PantsExecutionSettings fun(Pair<Project, String> projectStringPair) {
        final Project ideProject = projectStringPair.getFirst();
        final String projectPath = projectStringPair.getSecond();
        return getExecutionsSettingsFromPath(ideProject, projectPath);
      }

      @NotNull
      private PantsExecutionSettings getExecutionsSettingsFromPath(@NotNull Project ideProject, @NotNull String projectPath) {
        final AbstractExternalSystemSettings<?, ?, ?> systemSettings = ExternalSystemApiUtil.getSettings(ideProject, PantsConstants.SYSTEM_ID);
        final ExternalProjectSettings projectSettings = systemSettings.getLinkedProjectSettings(projectPath);

        if (projectSettings instanceof PantsProjectSettings) {
          PantsProjectSettings pantsProjectSettings = (PantsProjectSettings) projectSettings;
          return new PantsExecutionSettings(
            pantsProjectSettings.getProjectName(),
            pantsProjectSettings.getSelectedTargetSpecs(),
            pantsProjectSettings.libsWithSources,
            pantsProjectSettings.useIdeaProjectJdk,
            pantsProjectSettings.importSourceDepsAsJars,
            pantsProjectSettings.incrementalImportEnabled ? Optional.of(pantsProjectSettings.incrementalImportDepth) : Optional.empty(),
            pantsProjectSettings.useIntellijCompiler
          );
        }
        else {
          return PantsExecutionSettings.createDefault();
        }
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
    final Optional<PantsTargetAddress> addressOptional = PantsTargetAddress.fromString(targetProjectPath, false);
    assert addressOptional.isPresent();
    final PantsTargetAddress address = addressOptional.get();
    return address.getRelativePath() + ":" + address.getTargetName();
  }

  @Nullable
  @Override
  public FileChooserDescriptor getExternalProjectConfigDescriptor() {
    return BUILD_FILE_CHOOSER_DESCRIPTOR;
  }

  @Override
  public void enhanceRemoteProcessing(@NotNull SimpleJavaParameters parameters) {
    parameters.getVMParametersList().addProperty(
      ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, PantsConstants.SYSTEM_ID.getId()
    );
  }

  @Override
  public void runActivity(@NotNull Project project) {
  }
}
