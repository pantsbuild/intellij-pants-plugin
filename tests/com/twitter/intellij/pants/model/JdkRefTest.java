// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.google.common.collect.Sets;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.MockSdk;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.util.containers.MultiMap;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Set;

public class JdkRefTest extends LightPlatformTestCase {
  public void testResolvesToExistingSdk() {
    Sdk originalSdk = PantsUtil.createJdk("myName", "myHome", getTestRootDisposable());
    JdkRef ref = JdkRef.fromSdk(originalSdk);

    Sdk resolvedSdk = ref.toSdk(getTestRootDisposable());

    assertSame(originalSdk, resolvedSdk);
  }

  public void testRegistersSdkIfNotPresent() {
    Sdk sdk = new MockSdk("myName", "myHome", "myVersion", MultiMap.empty(), JavaSdk.getInstance());
    JdkRef ref = JdkRef.fromSdk(sdk);

    Sdk resolvedSdk = ref.toSdk(getTestRootDisposable());

    Set<Sdk> registeredSdks = Sets.newHashSet(ProjectJdkTable.getInstance().getAllJdks());
    assertTrue(registeredSdks.contains(resolvedSdk));
  }
}
