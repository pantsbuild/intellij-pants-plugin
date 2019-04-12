// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.twitter.intellij.pants.execution.DefaultRunConfigurationSelector;
import com.twitter.intellij.pants.service.project.PantsResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@State(
  name = "PantsSettings"
)
public class PantsSettings extends AbstractExternalSystemSettings<PantsSettings, PantsProjectSettings, PantsSettingsListener>
  implements PersistentStateComponent<PantsSettings.MyState> {

  protected boolean myUseIdeaProjectJdk = false;
  protected boolean myUsePantsMakeBeforeRun = true;
  protected int myResolverVersion = 0;
  protected DefaultRunConfigurationSelector.DefaultTestRunner myDefaultTestRunner;

  public PantsSettings(@NotNull Project project) {
    super(PantsSettingsListener.TOPIC, project);
  }

  @NotNull
  public static PantsSettings defaultSettings() {
    final PantsSettings pantsSettings = new PantsSettings(ProjectManager.getInstance().getDefaultProject());
    pantsSettings.setResolverVersion(PantsResolver.VERSION);
    return pantsSettings;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PantsSettings other = (PantsSettings) obj;
    return Objects.equals(myUseIdeaProjectJdk, other.myUseIdeaProjectJdk)
           && Objects.equals(myUsePantsMakeBeforeRun, other.myUsePantsMakeBeforeRun)
           && Objects.equals(myResolverVersion, other.myResolverVersion)
           && Objects.equals(myDefaultTestRunner, other.myDefaultTestRunner);
  }

  public static PantsSettings copy(PantsSettings pantsSettings) {
    PantsSettings settings = defaultSettings();
    settings.copyFrom(pantsSettings);
    return settings;
  }

  public static PantsSettings getSystemLevelSettings() {
    return getInstance(ProjectManager.getInstance().getDefaultProject());
  }

  public void setUseIdeaProjectJdk(boolean useIdeaProjectJdk) {
    myUseIdeaProjectJdk = useIdeaProjectJdk;
  }

  public boolean isUseIdeaProjectJdk() {
    return myUseIdeaProjectJdk;
  }

  public boolean isEnableIncrementalImport() {
    return getLinkedProjectsSettings().stream().anyMatch(PantsProjectSettings::isEnableIncrementalImport);
  }

  public void setEnableIncrementalImport(boolean enableIncrementalImport) {
    getLinkedProjectsSettings().forEach(s -> s.setEnableIncrementalImport(enableIncrementalImport));
  }

  public int getResolverVersion() {
    return myResolverVersion;
  }

  public void setResolverVersion(int resolverVersion) {
    myResolverVersion = resolverVersion;
  }

  public DefaultRunConfigurationSelector.DefaultTestRunner getDefaultTestRunner() {
    return myDefaultTestRunner;
  }

  public void setDefaultTestRunner(DefaultRunConfigurationSelector.DefaultTestRunner defaultTestRunner) {
    myDefaultTestRunner = defaultTestRunner;
  }

  @NotNull
  public static PantsSettings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, PantsSettings.class);
  }

  @Override
  public void subscribe(@NotNull ExternalSystemSettingsListener<PantsProjectSettings> listener) {

  }

  @Override
  protected void copyExtraSettingsFrom(@NotNull PantsSettings settings) {
    setResolverVersion(settings.getResolverVersion());
    setUseIdeaProjectJdk(settings.isUseIdeaProjectJdk());
    setDefaultTestRunner(settings.getDefaultTestRunner());
  }

  @Override
  protected void checkSettings(@NotNull PantsProjectSettings old, @NotNull PantsProjectSettings current) {
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public MyState getState() {
    final MyState state = new MyState();
    state.setResolverVersion(getResolverVersion());
    state.setUseIdeaProjectJdk(isUseIdeaProjectJdk());
    state.setDefaultTestRunner(getDefaultTestRunner());
    fillState(state);
    return state;
  }

  @Override
  public void loadState(MyState state) {
    super.loadState(state);
    setResolverVersion(state.getResolverVersion());
    setUseIdeaProjectJdk(state.isUseIdeaProjectJdk());
    setDefaultTestRunner(state.getDefaultTestRunner());
  }

  public static class MyState implements State<PantsProjectSettings> {
    Set<PantsProjectSettings> myLinkedExternalProjectsSettings = ContainerUtilRt.newTreeSet();

    boolean myUseIdeaProjectJdk = false;
    int myResolverVersion = 0;
    DefaultRunConfigurationSelector.DefaultTestRunner myDefaultTestRunner = DefaultRunConfigurationSelector.DefaultTestRunner.ALL;

    @AbstractCollection(surroundWithTag = false, elementTypes = {PantsProjectSettings.class})
    public Set<PantsProjectSettings> getLinkedExternalProjectsSettings() {
      return myLinkedExternalProjectsSettings;
    }

    public void setUseIdeaProjectJdk(boolean useIdeaProjectJdk) {
      myUseIdeaProjectJdk = useIdeaProjectJdk;
    }

    public boolean isUseIdeaProjectJdk() {
      return myUseIdeaProjectJdk;
    }

    public void setLinkedExternalProjectsSettings(Set<PantsProjectSettings> settings) {
      myLinkedExternalProjectsSettings = settings;
    }

    public int getResolverVersion() {
      return myResolverVersion;
    }

    public void setResolverVersion(int resolverVersion) {
      myResolverVersion = resolverVersion;
    }

    public DefaultRunConfigurationSelector.DefaultTestRunner getDefaultTestRunner() {
      return myDefaultTestRunner;
    }

    public void setDefaultTestRunner(DefaultRunConfigurationSelector.DefaultTestRunner defaultTestRunner) {
      myDefaultTestRunner = defaultTestRunner;
    }
  }
}
