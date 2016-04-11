// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.execution.RunManagerAdapter;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindowManager;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.components.PantsProjectComponent;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PantsProjectComponentImpl extends AbstractProjectComponent implements PantsProjectComponent {
  protected PantsProjectComponentImpl(Project project) {
    super(project);
  }

  @Override
  public void projectOpened() {
    super.projectOpened();
    if (myProject.isDefault()) {
      return;
    }

    StartupManager.getInstance(myProject).registerPostStartupActivity(
      new Runnable() {
        @Override
        public void run() {
          /**
           * Set project to allow dynamic classpath for JUnit run. Still requires any junit run to specify dynamic classpath in
           * {@link com.twitter.intellij.pants.execution.PantsClasspathRunConfigurationExtension#updateJavaParameters}
           * IDEA's logic: {@link com.intellij.execution.configurations.CommandLineBuilder}
           */
          PropertiesComponent.getInstance(myProject).setValue("dynamic.classpath", true);

          if (!PantsUtil.isPantsProject(myProject)) {
            /**
             * If a non-Pants project contains the following properties, we know it is launched from CLI
             * and will convert it to a Pants project.
             */
            String serializedTargets = PropertiesComponent.getInstance(myProject).getValue("targets");
            String projectPath = PropertiesComponent.getInstance(myProject).getValue("project_path");
            if (serializedTargets == null || projectPath == null) {
              return;
            }

            List<String> targetAddresses = PantsUtil.gson.fromJson(serializedTargets, PantsUtil.TYPE_LIST_STRING);
            ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(PantsConstants.SYSTEM_ID);
            AbstractExternalSystemSettings settings = manager.getSettingsProvider().fun(myProject);

            PantsProjectSettings pps = new PantsProjectSettings();
            pps.setTargetSpecs(targetAddresses);
            pps.setExternalProjectPath(projectPath);
            settings.setLinkedProjectsSettings(Collections.singleton(pps));

            PantsUtil.refreshAllProjects(myProject);
            // Open the project view explicitly.
            ToolWindowManager.getInstance(myProject).getToolWindow("Project").show(new Runnable() {
              @Override
              public void run() {
              }
            });
          }

          subscribeToRunConfigurationAddition();
          final AbstractExternalSystemSettings pantsSettings = ExternalSystemApiUtil.getSettings(myProject, PantsConstants.SYSTEM_ID);
          final boolean resolverVersionMismatch =
            pantsSettings instanceof PantsSettings && ((PantsSettings) pantsSettings).getResolverVersion() != PantsResolver.VERSION;
          if (resolverVersionMismatch && /* additional check */PantsUtil.isPantsProject(myProject)) {
            final int answer = Messages.showYesNoDialog(
              myProject,
              PantsBundle.message("pants.project.generated.with.old.version", myProject.getName()),
              PantsBundle.message("pants.name"),
              PantsIcons.Icon
            );
            if (answer == Messages.YES) {
              PantsUtil.refreshAllProjects(myProject);
            }
          }
        }

        private void subscribeToRunConfigurationAddition() {
          RunManagerEx.getInstanceEx(myProject).addRunManagerListener(
            new RunManagerAdapter() {
              @Override
              public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
                super.runConfigurationAdded(settings);
                if (!PantsUtil.isPantsProject(myProject)) {
                  return;
                }
                if (!PantsSettings.getInstance(myProject).isUsePantsMakeBeforeRun()) {
                  return;
                }
                PantsMakeBeforeRun.replaceDefaultMakeWithPantsMake(myProject, settings);
              }
            }
          );
        }
      }
    );
  }
}
