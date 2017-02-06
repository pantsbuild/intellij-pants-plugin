// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemToolWindowCondition;
import com.twitter.intellij.pants.util.PantsConstants;

public class PantsConsoleToolWindowFactoryCondition extends AbstractExternalSystemToolWindowCondition {
  public PantsConsoleToolWindowFactoryCondition() {
    super(PantsConstants.SYSTEM_ID);
  }
}
