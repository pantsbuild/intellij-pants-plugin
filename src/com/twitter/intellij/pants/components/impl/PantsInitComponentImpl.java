// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.components.PantsInitComponent;
import com.twitter.intellij.pants.ui.PantsCompileAllTargetsAction;
import com.twitter.intellij.pants.ui.PantsCompileAllTargetsInModuleAction;
import com.twitter.intellij.pants.ui.PantsRebuildAction;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.event.HyperlinkEvent;
import java.io.File;

public class PantsInitComponentImpl implements PantsInitComponent {
  @NotNull
  @Override
  public String getComponentName() {
    return "pants.init";
  }

  @Override
  public void initComponent() {
    // The Pants plugin doesn't do so many computations for building a project
    // to start an external JVM each time.
    // The plugin only calls `export` goal and parses JSON response.
    // So it will be in process all the time.

    final String key = PantsConstants.SYSTEM_ID.getId() + ExternalSystemConstants.USE_IN_PROCESS_COMMUNICATION_REGISTRY_KEY_SUFFIX;
    Registry.get(key).setValue(true);

    // hack to trick BuildProcessClasspathManager
    final String basePath = System.getProperty("pants.plugin.base.path");
    final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(PantsConstants.PLUGIN_ID));
    if (StringUtil.isNotEmpty(basePath) && plugin instanceof IdeaPluginDescriptorImpl) {
      ((IdeaPluginDescriptorImpl)plugin).setPath(new File(basePath));
    }

    registerRefreshKey();

    registerPantsActions();
  }

  @Override
  public void disposeComponent() {

  }

  //  Registers the rebuild action to Pants rebuild action.
  //  Registers Make module action to 'Make all targets in module' action.
  //  Disables compile action
  private void registerPantsActions() {
    ActionManager actionManager = ActionManager.getInstance();

    AnAction pantsCompileAllTargetAction = new PantsOverrideAction(
      PantsConstants.ACTION_MAKE_PROJECT_ID,
      PantsConstants.ACTION_MAKE_PROJECT_DESCRIPTION,
      new PantsCompileAllTargetsAction()
    );
    AnAction pantsMakeModuleAction = new PantsOverrideAction(
      IdeActions.ACTION_MAKE_MODULE,
      new PantsCompileAllTargetsInModuleAction()
    );
    AnAction pantsDisableCompileAction = PantsOverrideAction.createDisabledEmptyAction(IdeActions.ACTION_COMPILE);
    AnAction pantsRebuildAction = new PantsOverrideAction(
      IdeActions.ACTION_COMPILE_PROJECT,
      PantsConstants.REBUILD_PROJECT_DESCRIPTION,
      new PantsRebuildAction()
    );


    actionManager.unregisterAction(PantsConstants.ACTION_MAKE_PROJECT_ID);
    actionManager.unregisterAction(IdeActions.ACTION_MAKE_MODULE);
    actionManager.unregisterAction(IdeActions.ACTION_COMPILE);
    actionManager.unregisterAction(IdeActions.ACTION_COMPILE_PROJECT);

    actionManager.registerAction(PantsConstants.ACTION_MAKE_PROJECT_ID, pantsCompileAllTargetAction);
    actionManager.registerAction(IdeActions.ACTION_MAKE_MODULE, pantsMakeModuleAction);
    actionManager.registerAction(IdeActions.ACTION_COMPILE, pantsDisableCompileAction);
    actionManager.registerAction(IdeActions.ACTION_COMPILE_PROJECT, pantsRebuildAction);
  }

  //  Used to toggle between two actions, one that is active for Pants projects, and
  //  another for other projects.
  private static class PantsOverrideAction extends AnAction implements DumbAware {

    private AnAction secondaryIdeaAction;
    private AnAction primaryPantsAction;
    private boolean pantsActive;

    private PantsOverrideAction(String actionId, @NotNull AnAction pantsAction) {
      basicConstruction(actionId);
      primaryPantsAction = pantsAction;
    }

    private PantsOverrideAction(String actionId, String oldName, @NotNull AnAction pantsAction) {
      super(oldName);
      basicConstruction(actionId);
      primaryPantsAction = pantsAction;
    }


    private void basicConstruction(String actionId) {
      secondaryIdeaAction = getAction(actionId);
      primaryPantsAction = new PantsShieldAction(secondaryIdeaAction);
    }

    private static PantsOverrideAction createDisabledEmptyAction(String actionId) {
      return new PantsOverrideAction(actionId, new PantsShieldAction(getAction(actionId)));
    }

    private boolean isPantsProject(AnActionEvent event) {
      if (event == null) {
        return false;
      }
      Project project = event.getProject();
      final boolean result = project != null && PantsUtil.isPantsProject(project);
      pantsActive = result;
      return pantsActive;
    }

    @Override
    public void update(AnActionEvent event) {
      if (secondaryIdeaAction != null) {
        secondaryIdeaAction.update(event);
      }
      if (isPantsProject(event)) {
        event.getPresentation().setIcon(PantsIcons.Icon);
        primaryPantsAction.update(event);
      }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
      if (isPantsProject(event)) {
        primaryPantsAction.actionPerformed(event);
      }
      else if (secondaryIdeaAction != null) {
        secondaryIdeaAction.actionPerformed(event);
      }
    }

    @Override
    @TestOnly
    public String toString() {
      String activeOverride = pantsActive ? " actively" : "";
      return
        primaryPantsAction.getClass().getSimpleName() + activeOverride +  " overriding " +
        (secondaryIdeaAction == null ? "NullAction" : secondaryIdeaAction.getClass().getSimpleName());
    }
  }

  /**
   * Shield action allows previous action's text to still be displayed
   * and update, but disables all possible user interaction
   */
  private static class PantsShieldAction extends AnAction {
    private AnAction shieldedAction;

    private PantsShieldAction(@Nullable AnAction action) {
      shieldedAction = action;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {}

    @Override
    public void update(AnActionEvent event) {
      if (shieldedAction != null) {
        shieldedAction.update(event);
      }
      event.getPresentation().setEnabled(false);
    }
  }

  private static AnAction getAction(@NotNull String actionId) {
    return ActionManager.getInstance().getAction(actionId);
  }

  private void registerRefreshKey() {
    Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    KeyboardShortcut keyboardShortcut = KeyboardShortcut.fromString("shift meta pressed R");

    // Add (Cmd Shift R) as shortcut to refresh the project if there is no shortcut for that action yet.
    //  Shows error message if conflicting shortcut exists
    if (KeymapManager.getInstance().getActiveKeymap().getShortcuts("ExternalSystem.RefreshAllProjects").length == 0 &&
        keymap.getActionIds(keyboardShortcut).length > 0) {

      Notification notification = new Notification(
        "Keymap Error",
        "Keymap Error",
        "Conflict found assigning '⌘+Shift+R' to 'Refresh all external projects'. Please set it manually in " +
        "<a href='#'>Keymap settings</a>.",
        NotificationType.WARNING,
        new NotificationListener() {
          @Override
          public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
            ShowSettingsUtil.getInstance().showSettingsDialog(null, "Keymap");
          }
        }
      );
      Notifications.Bus.notify(notification);
      keymap.addShortcut("ExternalSystem.RefreshAllProjects", keyboardShortcut);
    }
  }
}
