// Copyright 2018 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.task;

import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.Optional;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState.BUILD_PROCESS_DEBUGGER_PORT_KEY;

public class PantsTaskManagerTest extends TestCase {

  public void testGetCleanedDebugSetup() {
    PantsExecutionSettings settings = new PantsExecutionSettings(Collections.emptyList(), false, false, false, Optional.empty(), false);
    settings.putUserData(BUILD_PROCESS_DEBUGGER_PORT_KEY, 63212);

    PantsTaskManager.setupDebuggerSettings(settings);
    assertEquals(1, settings.getJvmArguments().size());
    assertEquals("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=63212", settings.getJvmArguments().get(0));
  }
}
