// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.ide.actions.OpenProjectFileChooserDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Function;
import com.twitter.intellij.pants.service.project.PantsProjectResolver;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
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

/**
 * Created by fedorkorotkov
 */
public class PantsManager implements
                          ExternalSystemConfigurableAware,
                          ExternalSystemAutoImportAware,
                          ExternalSystemUiAware,
                          StartupActivity,
                          ExternalSystemManager<
                            PantsProjectSettings,
                            PantsSettingsListener,
                            PantsSettings,
                            PantsLocalSettings,
                            PantsExecutionSettings> {

  public static final FileChooserDescriptor BUILD_FILE_CHOOSER_DESCRIPTOR = new OpenProjectFileChooserDescriptor(true) {
    @Override
    public boolean isFileSelectable(VirtualFile file) {
      return PantsUtil.isPantsProjectFolder(file);
    }

    @Override
    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
      return super.isFileVisible(file, showHiddenFiles) && PantsUtil.isBUILDFileName(file.getName());
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
        final AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(ideProject, PantsConstants.SYSTEM_ID);

        final String projectPath = projectStringPair.getSecond();
        final ExternalProjectSettings projectSettings = systemSettings.getLinkedProjectSettings(projectPath);

        final List<String> targets = projectSettings instanceof PantsProjectSettings ?
                                     ((PantsProjectSettings)projectSettings).getTargets() : Collections.<String>emptyList();
        final boolean allTargets = projectSettings instanceof PantsProjectSettings &&
                                   ((PantsProjectSettings)projectSettings).isAllTargets();
        boolean compileWithIntellij = PantsSettings.getInstance(ideProject).isCompileWithIntellij();
        final PantsExecutionSettings executionSettings = new PantsExecutionSettings(targets, allTargets, compileWithIntellij);
        for (PantsResolverExtension resolver : PantsResolverExtension.EP_NAME.getExtensions()) {
          executionSettings.addResolverExtensionClassName(resolver.getClass().getName());
        }
        return executionSettings;
      }
    };
  }

  @NotNull
  @Override
  public Class<? extends ExternalSystemProjectResolver<PantsExecutionSettings>> getProjectResolverClass() {
    return PantsProjectResolver.class;
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
    return ExternalSystemApiUtil.getProjectRepresentationName(targetProjectPath, rootProjectPath);
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

  @Nullable
  @Override
  public String getAffectedExternalProjectPath(@NotNull String changedFileOrDirPath, @NotNull Project project) {
    VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(changedFileOrDirPath));
    if (virtualFile == null) {
      // changedFileOrDirPath might be relative
      final VirtualFile workingDir = PantsUtil.findPantsWorkingDir(project);
      virtualFile = workingDir != null ? workingDir.findFileByRelativePath(changedFileOrDirPath) : null;
    }
    if (virtualFile == null) {
      return null;
    }
    String pathKey = null;
    if (virtualFile.isDirectory()) {
      pathKey = ExternalSystemConstants.ROOT_PROJECT_PATH_KEY;
    } else if (PantsUtil.isBUILDFilePath(changedFileOrDirPath) || PantsUtil.isGeneratableFile(changedFileOrDirPath)) {
      pathKey = ExternalSystemConstants.LINKED_PROJECT_PATH_KEY;
    }

    // optimization check for pathKey != null
    final Module module = pathKey != null ? ModuleUtil.findModuleForFile(virtualFile, project) : null;
    return module != null ? module.getOptionValue(pathKey) : null;
  }
}
