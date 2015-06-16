// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.filters.Filter;
import com.intellij.openapi.command.impl.DummyProject;
import com.intellij.openapi.util.io.FileUtil;
import com.twitter.intellij.pants.util.PantsOutputMessage;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PantsOutputMessageTest extends UsefulTestCase {

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

  public List<PantsOutputMessage> parseCompilationOutputFile(String pathToFile) throws FileNotFoundException, IOException {
    assertNotNull(pathToFile);
    final List<String> lines = FileUtil.loadLines(new File(pathToFile));
    assertNotNull(lines);
    List<PantsOutputMessage> list = new ArrayList<PantsOutputMessage>();
    for (String line : lines) {
      list.add(PantsOutputMessage.parseOutputMessage(line));
    }
    return list;
  }

  public void testJavaSourceCompiledWithErrors() throws FileNotFoundException, IOException {
    final String pathToCompilationOutput = "testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/simpleCompilationOutput.txt";
    List<PantsOutputMessage> actualList = parseCompilationOutputFile(pathToCompilationOutput);
    assertNotNull(actualList);
    assertContainsElements(actualList, new PantsOutputMessage(
                                        25, 188,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/HelloWorld.java",
                                        10, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        25, 188,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/HelloWorld.java",
                                        16, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        25, 188,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/HelloWorld.java",
                                        16, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        25, 188,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/HelloWorld.java",
                                        16, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        25, 188,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/HelloWorld.java",
                                        16, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        25, 188,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/HelloWorld.java",
                                        16, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        25, 188,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/HelloWorld.java",
                                        17, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        25, 188,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/java/org/pantsbuild/testproject/failures/simple/HelloWorld.java",
                                        19, PantsOutputMessage.Level.ERROR)
    );
  }

  public void testScalaSourceCompiledWithErrors() throws FileNotFoundException, IOException {
    final String pathToCompilationOutput = "testData/testprojects/intellij-integration/src/scala/org/pantsbuild/testproject/failures/simple/simpleCompilationOutput.txt";
    List<PantsOutputMessage> actualList = parseCompilationOutputFile(pathToCompilationOutput);
    assertNotNull(actualList);
    assertContainsElements(actualList, new PantsOutputMessage(
                                        33, 198,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/scala/org/pantsbuild/testproject/failures/simple/HelloWorld.scala",
                                        12, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        33, 198,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/scala/org/pantsbuild/testproject/failures/simple/HelloWorld.scala",
                                        13, PantsOutputMessage.Level.ERROR),
                                       new PantsOutputMessage(
                                        33, 198,
                                        "/home/rushana/outreach/new9/intellij-pants-plugin/testData/testprojects/intellij-integration/src/scala/org/pantsbuild/testproject/failures/simple/HelloWorld.scala",
                                        18, PantsOutputMessage.Level.ERROR)
   );
  }
}
