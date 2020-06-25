// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.EditorNotificationPanel;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.bsp.BspUtil;

import java.util.Collections;
import java.util.Optional;

class ConvertToSourceDependencyEditorNotificationsProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
  final Logger logger = Logger.getInstance(ConvertToSourceDependencyEditorNotificationsProvider.class);
  private static final Key<EditorNotificationPanel> KEY = Key.create("fastpass.amend.notification");

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(
    @NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project
  ) {
    if(!BspUtil.isBspProject(project)){
      return null;
    }
    JarMappings mappings = JarMappings.getInstance(project);
    Optional<PantsTargetAddress> targetName =
      JarMappings.getParentJar(file)
        .flatMap(mappings::findTargetForJar)
        .flatMap(PantsTargetAddress::tryParse);
    if (targetName.isPresent()) {
      EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.createActionLabel(PantsBundle.message("pants.bsp.editor.convert.button"), () -> {
        try {
          OpenBspAmendWindowAction.bspAmendWithDialog(project, Collections.singleton(targetName.get().toAddressString()));
        } catch (Throwable e) {
          logger.error(e);
        }
      });
      return panel.text(PantsBundle.message(
        "pants.bsp.file.editor.amend.notification.title",
        targetName.get().toAddressString()
      ));
    }
    else {
      return null;
    }
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }
}