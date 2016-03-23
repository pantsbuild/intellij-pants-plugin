// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.execution.*;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.components.PantsProjectComponent;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

import java.util.ArrayList;
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

          subscribeToRunConfigurationAddition();
          final AbstractExternalSystemSettings pantsSettings = ExternalSystemApiUtil.getSettings(myProject, PantsConstants.SYSTEM_ID);
          final boolean resolverVersionMismatch =
            pantsSettings instanceof PantsSettings && ((PantsSettings)pantsSettings).getResolverVersion() != PantsResolver.VERSION;
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
                RunManager runManager = RunManager.getInstance(myProject);
                if (!(runManager instanceof RunManagerImpl)) {
                  return;
                }
                RunManagerImpl runManagerImpl = (RunManagerImpl)runManager;
                RunConfiguration runConfiguration = settings.getConfiguration();

                VirtualFile buildRoot = PantsUtil.findBuildRoot(myProject);
                if (runConfiguration instanceof JUnitConfiguration) {
                  if (buildRoot != null) {
                    ((JUnitConfiguration)runConfiguration).setWorkingDirectory(buildRoot.getPath());
                  }
                }
                else if (runConfiguration instanceof ScalaTestRunConfiguration) {
                  if (buildRoot != null) {
                    ((ScalaTestRunConfiguration)runConfiguration).setWorkingDirectory(buildRoot.getPath());
                  }
                }
                else {
                  return;
                }
                /**
                 * Every time a new configuration is created, 'Make' is by default added to the "Before launch" tasks.
                 * Therefore we want to remove it by preserving only {@link PantsMakeBeforeRun}.
                 */
                List<BeforeRunTask> beforeRunTasks = runManagerImpl.getBeforeRunTasks(settings.getConfiguration());
                List<BeforeRunTask> newTasks = new ArrayList<BeforeRunTask>();
                for (BeforeRunTask task : beforeRunTasks) {
                  if (task.getProviderId().equals(PantsMakeBeforeRun.ID)) {
                    newTasks.add(task);
                  }
                }
                runManagerImpl.setBeforeRunTasks(runConfiguration, newTasks, false);
              }
            }
          );
        }
      }
    );
  }
}
