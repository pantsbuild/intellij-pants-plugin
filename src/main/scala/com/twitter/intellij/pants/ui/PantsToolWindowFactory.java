// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory;
import com.twitter.intellij.pants.util.PantsConstants;

public class PantsToolWindowFactory extends AbstractExternalSystemToolWindowFactory {
  public PantsToolWindowFactory() {
    super(PantsConstants.SYSTEM_ID);
  }
}
