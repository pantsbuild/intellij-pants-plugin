// Copyright 2018 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.task;

import junit.framework.TestCase;

public class PantsTaskManagerTest extends TestCase {

  public void testGetCleanedDebugSetup() {
    String cleanedSetup =
      PantsTaskManager.getCleanedDebugSetup("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=63212 -forkSocket63213");
    assertEquals("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=63212", cleanedSetup);
  }
}
