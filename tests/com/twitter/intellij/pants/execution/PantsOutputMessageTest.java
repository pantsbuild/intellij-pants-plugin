// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.filters.Filter;
import com.intellij.openapi.command.impl.DummyProject;
import com.twitter.intellij.pants.util.PantsOutputMessage;
import junit.framework.TestCase;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PantsOutputMessageTest extends TestCase {

  public void doTest(@Nullable PantsOutputMessage expected, @Nullable PantsOutputMessage actual) {
    if (expected == null) {
      assertNull(actual);
    }
    else {
      assertNotNull(actual);
      assertEquals(expected.getStart(), actual.getStart());
      assertEquals(expected.getEnd(), actual.getEnd());
      assertEquals(expected.getFilePath(), actual.getFilePath());
      assertEquals(expected.getLineNumber(), actual.getLineNumber());
    }
  }

  public void testBadMessage() {
    PantsOutputMessage info = PantsOutputMessage.parseMessage("bla bla bla", true, false);
    assertNull(info);
  }

  public void testUrl() {
    PantsOutputMessage info = PantsOutputMessage.parseOutputMessage("/this/is/a/url");
    doTest(new PantsOutputMessage(0, 14, "/this/is/a/url", 0), info);
  }

  public void testUrlWithSpaces() {
    PantsOutputMessage info = PantsOutputMessage.parseOutputMessage("     /this/is/a/url");
    doTest(new PantsOutputMessage(5, 19, "/this/is/a/url", 0), info);
  }

  public void testUrlWithLineNumber() {
    PantsOutputMessage info = PantsOutputMessage.parseOutputMessage("/this/is/a/url:23");
    doTest(new PantsOutputMessage(0, 17, "/this/is/a/url", 22), info);
  }

  public void testUrlWithLineNumberAndMessage() {
    PantsOutputMessage info = PantsOutputMessage.parseOutputMessage("/this/is/a/url:23: error: ...");
    doTest(new PantsOutputMessage(0, 17, "/this/is/a/url", 22, PantsOutputMessage.Level.ERROR), info);
  }

  public void testUrlWithSpaceAndNumberAndMessage() {
    PantsOutputMessage info = PantsOutputMessage.parseOutputMessage("     /this/is/a/url:23: message ...");
    doTest(new PantsOutputMessage(5, 22, "/this/is/a/url", 22), info);
  }

  public void testUrlWithTabsAndNumberAndMessage() {
    PantsOutputMessage info = PantsOutputMessage.parseOutputMessage("\t/this/is/a/url:23: message ...");
    doTest(new PantsOutputMessage(1, 18, "/this/is/a/url", 22), info);
  }

  public void testUrlWithErrorInBrackets() {
    PantsOutputMessage info = PantsOutputMessage.parseOutputMessage("     [error] /this/is/a/url");
    doTest(new PantsOutputMessage(13, 27, "/this/is/a/url", 0, PantsOutputMessage.Level.ERROR), info);
  }

  public void testDotFilePathHasNullFilterResult() {
    PantsFilter filter = new PantsFilter(DummyProject.getInstance());
    Filter.Result result = filter.applyFilter(".: message", 10000);
    assertNull("result", result);
  }

  protected static <T> void assertContain(List<? extends T> actual, T... expected) {
    List expectedList = Arrays.asList(expected);
    assertTrue("expected: " + expectedList + "\n" + "actual: " + actual.toString(), actual.containsAll(expectedList));
  }

  public List<PantsOutputMessage> parseCompilationOutputFile(String pathToFile) throws FileNotFoundException, IOException {
    assertNotNull(pathToFile);
    BufferedReader br = new BufferedReader(new FileReader(new File(pathToFile)));
    List<PantsOutputMessage> list = new ArrayList<PantsOutputMessage>();
    for (String line; (line = br.readLine()) != null; ) {
      list.add(PantsOutputMessage.parseOutputMessage(line));
    }
    return list;
  }

  public void testJavaSourceCompiledWithError() throws FileNotFoundException, IOException {
    String pathToCompilationOutput = new String("testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/simple/simpleCompilationOutput.txt");
    List<PantsOutputMessage> list = parseCompilationOutputFile(pathToCompilationOutput);
    assertNotNull(list);
    assertContain(
      list, new PantsOutputMessage(
                25,
                179,
                "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/simple/HelloWorld.java",
                10,
                PantsOutputMessage.Level.ERROR)
    );
  }
}
