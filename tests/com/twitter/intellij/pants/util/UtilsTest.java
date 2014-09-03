package com.twitter.intellij.pants.util;

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import junit.framework.TestCase;

public class UtilsTest extends TestCase{

  public void testSourceTypeForTargetType() {
    assertEquals("Source type correctly set",
                 ExternalSystemSourceType.SOURCE,
                 Utils.getSourceTypeForTargetType("source"));

    assertEquals("Resource type correctly set",
                 ExternalSystemSourceType.RESOURCE,
                 Utils.getSourceTypeForTargetType("resource"));

    assertEquals("Test Source type correctly set",
                 ExternalSystemSourceType.TEST,
                 Utils.getSourceTypeForTargetType("TEST"));

    assertEquals("Test Resource type correctly set",
                 ExternalSystemSourceType.TEST_RESOURCE,
                 Utils.getSourceTypeForTargetType("TEST_RESOURCE"));

    assertEquals("Source type correctly set for gibberish",
                 ExternalSystemSourceType.SOURCE,
                 Utils.getSourceTypeForTargetType("gibberish"));
  }
}
