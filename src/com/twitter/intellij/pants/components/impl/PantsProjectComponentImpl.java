// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationProducer;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.components.PantsProjectComponent;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;

import java.util.Arrays;
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
          //RunManager.getInstance(myProject).getConfigurationFactories()
          //RunManager.getInstance(myProject).getConfigurationTemplate()
          //
          //RunManager.getInstance(myProject).getAllConfigurationsList();
          //
          //RunManager.getInstance(myProject).getConfigurationTemplate(ConfigurationFactory)
          RunManager runManager = RunManager.getInstance(myProject);
          if (runManager instanceof RunManagerImpl) {
            RunManagerImpl runManagerImpl = (RunManagerImpl) runManager;
            for (RunConfiguration runConfiguration : runManagerImpl.getAllConfigurationsList()) {
              List<BeforeRunTask> beforeRunTasks = runManagerImpl.getBeforeRunTasks(runConfiguration);
              List<BeforeRunTask> newTasks = Arrays.asList(beforeRunTasks.iterator().next());

              //runManagerImpl.getBeforeRunTasks(MakeProjectStepBeforeRun.ID)
              runManagerImpl.resetBeforeRunTasks(runConfiguration);
              runManagerImpl.setBeforeRunTasks(runConfiguration, Collections.<BeforeRunTask>emptyList(), false);
              //RunManagerEx.getInstanceEx(myProject).setBeforeRunTasks(runConfiguration, Collections.<BeforeRunTask>emptyList(), false);
              //runManagerImpl.setBeforeRunTasks(runConfiguration, newTasks, true);
              //JUnitConfigurationProducer.getProducers(myProject).


              int x = 5;

              //beforeRunTasks.add(new MyBeforeRunTask(providerID));
            }
          }


          PropertiesComponent.getInstance(myProject).setValue("dynamic.classpath", true);
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
      }
    );
  }
}
