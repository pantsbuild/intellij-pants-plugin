// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.intellij.testFramework.UsefulTestCase;
import com.twitter.intellij.pants.psi.reference.PantsTargetReferenceSet.PartialTargetAddress;


public class PartialTargetAddressTest extends UsefulTestCase {
  public void testAddressWithoutTarget() {
    PartialTargetAddress address = PartialTargetAddress.parse("123/abc/efg/");
    assertEquals(address.getExplicitTarget(), null);
    assertEquals(address.getNormalizedPath(), "123/abc/efg/");
  }

  public void testAddressWithTarget() {
    PartialTargetAddress address = PartialTargetAddress.parse("123/abc/efg:core");
    assertEquals(address.getExplicitTarget(), "core");
    assertEquals(address.getNormalizedPath(), "123/abc/efg/");
  }

  public void testAddressIncomplete() {
    PartialTargetAddress address = PartialTargetAddress.parse("123/abc/efg:");
    assertEquals(address.getExplicitTarget(), null);
    assertEquals(address.getNormalizedPath(), "123/abc/efg/");
  }
}
