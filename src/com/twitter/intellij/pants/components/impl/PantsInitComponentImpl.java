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
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.components.PantsInitComponent;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

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

    ActionManager actionManager = ActionManager.getInstance();
    //DefaultActionGroup actionGroup = new DefaultActionGroup(actionManager.getAction("Compile"));
    //actionGroup.remove(actionGroup.getChildActionsOrStubs()[0]);
    DefaultActionGroup actions = (DefaultActionGroup) actionManager.getAction("ProjectViewCompileGroup");
    for (AnAction action : actions.getChildActionsOrStubs()) {
      if (action != null && action.toString().contains("Make")) {
        actions.remove(action);
      }
      System.out.println(actionManager.getId(action));
      System.out.println(action);
    }

  }

  @Override
  public void disposeComponent() {

  }
}