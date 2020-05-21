// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.google.common.collect.Sets;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.xmlb.annotations.XCollection;
import com.twitter.intellij.pants.service.project.PantsResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@State(
  name = "PantsSettings"
)
public class PantsSettings extends AbstractExternalSystemSettings<PantsSettings, PantsProjectSettings, PantsSettingsListener>
  implements PersistentStateComponent<PantsSettings.MyState> {

  protected boolean myUseIdeaProjectJdk = false;
  protected boolean myUsePantsMakeBeforeRun = true;
  protected int myResolverVersion = 0;

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
  public int hashCode() {
    return Objects.hash(myUseIdeaProjectJdk, myUsePantsMakeBeforeRun, myResolverVersion);
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
           && Objects.equals(myResolverVersion, other.myResolverVersion);
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
    return getLinkedProjectsSettings().stream().anyMatch(x -> x.incrementalImportDepth().isPresent());
  }


  public boolean isUseIntellijCompiler() {
    return getLinkedProjectsSettings().stream().anyMatch(s -> s.useIntellijCompiler);
  }
  public void setEnableIncrementalImport(Optional<Integer> incrementalImportDepth) {
    getLinkedProjectsSettings().forEach(s ->
                                        {
                                          s.incrementalImportEnabled = incrementalImportDepth.isPresent();
                                          s.incrementalImportDepth = incrementalImportDepth.orElse(0);
                                        });
  }

  public int getResolverVersion() {
    return myResolverVersion;
  }

  public void setResolverVersion(int resolverVersion) {
    myResolverVersion = resolverVersion;
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
  }

  @Override
  protected void checkSettings(@NotNull PantsProjectSettings old, @NotNull PantsProjectSettings current) {
  }

  @Nullable
  @Override
  public MyState getState() {
    final MyState state = new MyState();
    state.setResolverVersion(getResolverVersion());
    state.setUseIdeaProjectJdk(isUseIdeaProjectJdk());
    fillState(state);
    return state;
  }

  @Override
  public void loadState(@NotNull MyState state) {
    super.loadState(state);
    setResolverVersion(state.getResolverVersion());
    setUseIdeaProjectJdk(state.isUseIdeaProjectJdk());
  }

  public static class MyState implements State<PantsProjectSettings> {
    Set<PantsProjectSettings> myLinkedExternalProjectsSettings = Sets.newTreeSet();

    boolean myUseIdeaProjectJdk = false;
    int myResolverVersion = 0;

    @XCollection(elementTypes = {PantsProjectSettings.class})
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
  }
}
