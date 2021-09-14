// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package org.pantsbuild.testproject;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DummyTest {
    @Test
    public void alwaysWorks() {
        Dummy x = new Dummy();
        System.out.println(x.hashCode());
        assertTrue(true);
    }
}
