// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.google.common.collect.Sets;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.MockSdk;
import com.intellij.util.containers.MultiMap;
import com.twitter.intellij.pants.testFramework.PantsIntegrationTestCase;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Set;

public class JdkRefTest extends PantsIntegrationTestCase {
  private static final String SDK_NAME = "mySDK";

  public void testResolvesToExistingSdk() {
    Sdk originalSdk = PantsUtil.createJdk(SDK_NAME, "myHome", getTestRootDisposable());
    JdkRef ref = JdkRef.fromSdk(originalSdk);

    Sdk resolvedSdk = ref.toSdk(getTestRootDisposable());

    assertSame(originalSdk, resolvedSdk);
  }

  public void testRegistersSdkIfNotPresent() {
    Sdk sdk = new MockSdk(SDK_NAME, "myHome", "myVersion", MultiMap.empty(), JavaSdk.getInstance());
    JdkRef ref = JdkRef.fromSdk(sdk);

    Sdk resolvedSdk = ref.toSdk(getTestRootDisposable());

    Set<Sdk> registeredSdks = Sets.newHashSet(ProjectJdkTable.getInstance().getAllJdks());
    assertTrue(registeredSdks.contains(resolvedSdk));
  }

  @Override
  public void tearDown() throws Exception {
    removeJdks(jdk -> jdk.getName().equals(SDK_NAME));
    super.tearDown();
  }
}
