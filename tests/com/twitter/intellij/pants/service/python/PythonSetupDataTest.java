// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python;

import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.serialization.ObjectSerializer;
import com.intellij.serialization.ReadConfiguration;
import com.twitter.intellij.pants.service.project.model.PythonInterpreterInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

public final class PythonSetupDataTest extends TestCase {
  public void testCanBeSerialized() {
    PythonSetupData expected = newData();

    PythonSetupData actual = deserialized(expected);
    assertEquals(expected, actual);
  }

  private PythonSetupData deserialized(PythonSetupData data) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    new ObjectSerializer().write(data, output);
    return new ObjectSerializer().read(PythonSetupData.class, output.toByteArray(), new ReadConfiguration());
  }

  @NotNull
  private PythonSetupData newData() {
    PythonInterpreterInfo info = new PythonInterpreterInfo();
    ModuleData moduleData = new ModuleData("id", PantsConstants.SYSTEM_ID, "module", "name", "path", "path");
    info.setBinary("binary");
    info.setChroot("chroot");
    return new PythonSetupData(moduleData, info);
  }
}