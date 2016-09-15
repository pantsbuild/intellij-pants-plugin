// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.testFramework.LightPlatformTestCase;

public class SimpleExportResultTest extends LightPlatformTestCase {
  private static boolean STRICT = true;
  public void testParseExport_1_0_7() throws Exception {
    final String exportOutput =
      "{\n" +
      "    \"libraries\": {},\n" +
      "    \"version\": \"1.0.7\",\n" +
      "    \"targets\": {},\n" +
      "    \"preferred_jvm_distributions\": {\n" +
      "        \"java7\": {\n" +
      "            \"strict\": \"/Library/Java/JavaVirtualMachines/jdk1.7.0_72.jdk/Contents/Home\",\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.7.0_72.jdk/Contents/Home\"\n" +
      "        },\n" +
      "        \"java6\": {\n" +
      "            \"strict\": \"/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home\",\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.7.0_72.jdk/Contents/Home\"\n" +
      "        },\n" +
      "        \"java8\": {\n" +
      "            \"strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home\",\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home\"\n" +
      "        }\n" +
      "    },\n" +
      "    \"jvm_platforms\": {\n" +
      "        \"platforms\": {\n" +
      "            \"java7\": {\n" +
      "                \"source_level\": \"1.7\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.7\"\n" +
      "            },\n" +
      "            \"java6\": {\n" +
      "                \"source_level\": \"1.6\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.6\"\n" +
      "            },\n" +
      "            \"java8\": {\n" +
      "                \"source_level\": \"1.8\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.8\"\n" +
      "            }\n" +
      "        },\n" +
      "        \"default_platform\": \"java6\"\n" +
      "    }\n" +
      "}";

    SimpleExportResult exportResult = SimpleExportResult.parse(exportOutput);
    assertEquals("1.0.7", exportResult.getVersion());
    assertEquals("java6", exportResult.getJvmPlatforms().getDefaultPlatform());
    assertEquals(
      "/Library/Java/JavaVirtualMachines/jdk1.7.0_72.jdk/Contents/Home",
      exportResult.getJdkHome(!STRICT).get()
    );
    assertEquals(
      "/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home",
      exportResult.getJdkHome(STRICT).get()
    );
    assertTrue(exportResult.getJdkHome(STRICT).isPresent());
  }

  public void testParseExport_1_0_6() throws Exception {
    final String exportOutput =
      "{\n" +
      "    \"libraries\": {},\n" +
      "    \"version\": \"1.0.6\",\n" +
      "    \"targets\": {},\n" +
      "    \"jvm_platforms\": {\n" +
      "        \"platforms\": {\n" +
      "            \"java7\": {\n" +
      "                \"source_level\": \"1.7\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.7\"\n" +
      "            },\n" +
      "            \"java6\": {\n" +
      "                \"source_level\": \"1.6\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.6\"\n" +
      "            },\n" +
      "            \"java8\": {\n" +
      "                \"source_level\": \"1.8\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.8\"\n" +
      "            }\n" +
      "        },\n" +
      "        \"default_platform\": \"java6\"\n" +
      "    }\n" +
      "}";
    SimpleExportResult exportResult = SimpleExportResult.parse(exportOutput);
    assertEquals("1.0.6", exportResult.getVersion());
    assertEquals("java6", exportResult.getJvmPlatforms().getDefaultPlatform());
    assertNull(exportResult.getPreferredJvmDistributions());
  }

  public void testExportCache() {
    SimpleExportResult export_a = SimpleExportResult.getExportResult("./pants");
    SimpleExportResult export_b = SimpleExportResult.getExportResult("./pants");
    // export_b should be cached result, so identical to export_a
    assertTrue(export_a == export_b);
    SimpleExportResult.clearCache();
    SimpleExportResult export_c = SimpleExportResult.getExportResult("./pants");
    assertTrue(export_a != export_c);
  }

  public void testMissingStrict() {
    final String exportOutput =
      "{\n" +
      "    \"preferred_jvm_distributions\": {\n" +
      "        \"java7\": {\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_102.jdk/Contents/Home\"\n" +
      "        },\n" +
      "        \"java7-jdk8\": {\n" +
      "            \"strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_102.jdk/Contents/Home\",\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_102.jdk/Contents/Home\"\n" +
      "        },\n" +
      "        \"java8\": {\n" +
      "            \"strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_102.jdk/Contents/Home\",\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_102.jdk/Contents/Home\"\n" +
      "        }\n" +
      "    },\n" +
      "    \"libraries\": {},\n" +
      "    \"version\": \"1.0.7\",\n" +
      "    \"targets\": {},\n" +
      "    \"jvm_platforms\": {\n" +
      "        \"platforms\": {\n" +
      "            \"java7\": {\n" +
      "                \"source_level\": \"1.7\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.7\"\n" +
      "            },\n" +
      "            \"java7-jdk8\": {\n" +
      "                \"source_level\": \"1.7\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.8\"\n" +
      "            },\n" +
      "            \"java8\": {\n" +
      "                \"source_level\": \"1.8\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.8\"\n" +
      "            }\n" +
      "        },\n" +
      "        \"default_platform\": \"java7\"\n" +
      "    }\n" +
      "}";
    SimpleExportResult exportResult = SimpleExportResult.parse(exportOutput);
    // java7 has no strict jdk home path.
    assertFalse(exportResult.getJdkHome(STRICT).isPresent());
  }

  public void testNoDefaultPlatform() {
    final String exportOutput =
      "{\n" +
      "    \"preferred_jvm_distributions\": {\n" +
      "        \"java7\": {\n" +
      "            \"strict\": \"/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home\",\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home\"\n" +
      "        },\n" +
      "        \"java6\": {\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home\"\n" +
      "        },\n" +
      "        \"java8\": {\n" +
      "            \"strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home\",\n" +
      "            \"non_strict\": \"/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home\"\n" +
      "        }\n" +
      "    },\n" +
      "    \"libraries\": {},\n" +
      "    \"version\": \"1.0.9\",\n" +
      "    \"targets\": {},\n" +
      "    \"jvm_platforms\": {\n" +
      "        \"platforms\": {\n" +
      "            \"java7\": {\n" +
      "                \"source_level\": \"1.7\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.7\"\n" +
      "            },\n" +
      "            \"java6\": {\n" +
      "                \"source_level\": \"1.6\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.6\"\n" +
      "            },\n" +
      "            \"java8\": {\n" +
      "                \"source_level\": \"1.8\",\n" +
      "                \"args\": [],\n" +
      "                \"target_level\": \"1.8\"\n" +
      "            }\n" +
      "        },\n" +
      "        \"default_platform\": \"(DistributionLocator.cached().version 1.8)\"\n" +
      "    }\n" +
      "}";
    SimpleExportResult exportResult = SimpleExportResult.parse(exportOutput);
    assertFalse(exportResult.getJdkHome(STRICT).isPresent());
  }
}
