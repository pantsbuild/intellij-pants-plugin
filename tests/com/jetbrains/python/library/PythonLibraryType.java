// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.jetbrains.python.library;

import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Dummy class to avoid ClassNotFound exception in PantsCompletionTest
 * when we try to set up a Python plugin.
 * <p/>
 * Python Community plugin.xml is corrupted but we can't do anything about this.
 */
public class PythonLibraryType extends LibraryType<LibraryVersionProperties> {
  public PythonLibraryType() {
    super(
      new PersistentLibraryKind<LibraryVersionProperties>("dummy.type") {
        @NotNull
        @Override
        public LibraryVersionProperties createDefaultProperties() {
          return new LibraryVersionProperties();
        }
      }
    );
  }

  @Nullable
  @Override
  public String getCreateActionName() {
    return null;
  }

  @Nullable
  @Override
  public NewLibraryConfiguration createNewLibrary(
    @NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory, @NotNull Project project
  ) {
    return null;
  }

  @Nullable
  @Override
  public LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<LibraryVersionProperties> editorComponent) {
    return null;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return null;
  }
}
