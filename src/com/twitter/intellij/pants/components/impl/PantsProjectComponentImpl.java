// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.compiler.server.BuildManagerListener;
import com.intellij.execution.RunManagerAdapter;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.messages.MessageBusConnection;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.components.PantsProjectComponent;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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

          registerExternalBuilderListener();
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

  /**
   * This registers the listener when IDEA external builder process calls Pants.
   */
  private void registerExternalBuilderListener() {
    MessageBusConnection connection = myProject.getMessageBus().connect();
    BuildManagerListener buildManagerListener = new BuildManagerListener() {
      @Override
      public void beforeBuildProcessStarted(Project project, UUID sessionId) {

      }

      @Override
      public void buildStarted(Project project, UUID sessionId, boolean isAutomake) {

      }

      @Override
      public void buildFinished(Project project, UUID sessionId, boolean isAutomake) {
        /**
         * Sync files as generated sources may have changed after external compile,
         * specifically when {@link com.twitter.intellij.pants.jps.incremental.PantsTargetBuilder} finishes,
         * except this code is run within IDEA core, thus having access to file sync calls.
         */
        PantsUtil.synchronizeFiles();
      }
    };
    connection.subscribe(BuildManagerListener.TOPIC, buildManagerListener);
  }
}
