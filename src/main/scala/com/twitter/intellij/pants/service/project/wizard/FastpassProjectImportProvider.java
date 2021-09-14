// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.PropertiesComponent;
import icons.PantsIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.bsp.project.importing.BspProjectImportProvider;

import javax.swing.Icon;
import java.util.Optional;

public class FastpassProjectImportProvider extends BspProjectImportProvider {
  static public String label() {
    return Optional
      .ofNullable(PropertiesComponent.getInstance().getValue("fastpass.import.provider.label"))
      .orElse("Pants (Fastpass)");
  }

  @Override
  public @Nullable Icon getIcon() {
    return PantsIcons.Icon;
  }

  @NotNull
  @Override
  public String getId() {
    return "Fastpass";
  }

  @Override
  public @NotNull
  @Nls(capitalization = Nls.Capitalization.Sentence) String getName() {
    return label();
  }
}
