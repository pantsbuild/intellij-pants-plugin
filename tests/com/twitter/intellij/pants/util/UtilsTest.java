package com.twitter.intellij.pants.util;

import junit.framework.TestCase;

public class UtilsTest extends TestCase{

  public void testSourceTypeForTargetType() {
    assertEquals("Source type correctly set",
                 PantsSourceType.SOURCE,
                 PantsUtil.getSourceTypeForTargetType("source"));

    assertEquals("Resource type correctly set",
                 PantsSourceType.RESOURCE,
                 PantsUtil.getSourceTypeForTargetType("resource"));

    assertEquals("Test Source type correctly set",
                 PantsSourceType.TEST,
                 PantsUtil.getSourceTypeForTargetType("TEST"));

    assertEquals("Test Resource type correctly set",
                 PantsSourceType.TEST_RESOURCE,
                 PantsUtil.getSourceTypeForTargetType("TEST_RESOURCE"));

    assertEquals("Source type correctly set for gibberish",
                 PantsSourceType.SOURCE,
                 PantsUtil.getSourceTypeForTargetType("gibberish"));
  }
}
