// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.common.annotations.VisibleForTesting;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetInfoTest extends TestCase {

  public void test1() {
    TargetInfo info = createTargetInfoWithTargetAddressInfo("resource", "source");
    assertEquals(PantsSourceType.SOURCE, info.getSourcesType());
  }

  public void test2() {
    TargetInfo info = createTargetInfoWithTargetAddressInfo("test", "source");
    assertEquals(PantsSourceType.SOURCE, info.getSourcesType());
  }

  public void test3() {
    TargetInfo info = createTargetInfoWithTargetAddressInfo("test", "resource", "test_resource");
    assertEquals(PantsSourceType.TEST, info.getSourcesType());
  }

  public void test4() {
    TargetInfo info = createTargetInfoWithTargetAddressInfo("test_resource", "resource", "resource");
    assertEquals(PantsSourceType.RESOURCE, info.getSourcesType());
  }

  public void test5() {
    TargetInfo info = createTargetInfoWithTargetAddressInfo("test_resource", "test_resource");
    assertEquals(PantsSourceType.TEST_RESOURCE, info.getSourcesType());
  }

  public void testInvalid() {
    TargetInfo info = createTargetInfoWithTargetAddressInfo("invalid_type", "test_resource");
    assertEquals(PantsSourceType.SOURCE, info.getSourcesType());
  }

  @VisibleForTesting
  public static TargetInfo createTargetInfoWithTargetAddressInfo(String... types) {
    Set<TargetAddressInfo> targetAddressInfoSet = Arrays.stream(types).map(s -> {
      TargetAddressInfo x = new TargetAddressInfo();
      x.setTargetType(s);
      return x;
    }).collect(Collectors.toSet());
    TargetInfo dummy = new TargetInfo();
    dummy.setAddressInfos(targetAddressInfoSet);
    return dummy;
  }
}
