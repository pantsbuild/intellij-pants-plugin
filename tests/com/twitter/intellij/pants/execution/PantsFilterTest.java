package com.twitter.intellij.pants.execution;

import com.twitter.intellij.pants.execution.PantsFilter.PantsFilterInfo;
import junit.framework.TestCase;
import org.jetbrains.annotations.Nullable;


/**
 * Created by ajohnson on 6/19/14.
 */
public class PantsFilterTest extends TestCase {//LightCodeInsightFixtureTestCase {

  public void doTest(@Nullable PantsFilterInfo expected, @Nullable PantsFilterInfo actual) {
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

  public void testUrl() {
    PantsFilterInfo info = PantsFilter.parseLine("/this/is/a/url");
    doTest(new PantsFilterInfo(0, 14, "/this/is/a/url", 0), info);
  }

  public void testUrlWithSpaces() {
    PantsFilterInfo info = PantsFilter.parseLine("     /this/is/a/url");
    doTest(new PantsFilterInfo(5, 19, "/this/is/a/url", 0), info);
  }

  public void testUrlWithLineNumber() {
    PantsFilterInfo info = PantsFilter.parseLine("/this/is/a/url:23");
    doTest(new PantsFilterInfo(0, 17, "/this/is/a/url", 22), info);
  }

  public void testUrlWithLineNumberAndMessage() {
    PantsFilterInfo info = PantsFilter.parseLine("/this/is/a/url:23: error: ...");
    doTest(new PantsFilterInfo(0, 17, "/this/is/a/url", 22), info);
  }

  public void testUrlWithSpaceAndNumberAndMessage() {
    PantsFilterInfo info = PantsFilter.parseLine("     /this/is/a/url:23: message ...");
    doTest(new PantsFilterInfo(5, 22, "/this/is/a/url", 22), info);
  }

  public void testUrlWithTabsAndNumberAndMessage() {
    PantsFilterInfo info = PantsFilter.parseLine("\t/this/is/a/url:23: message ...");
    doTest(new PantsFilterInfo(1, 18, "/this/is/a/url", 22), info);
  }

  public void testUrlWithErrorInBrackets() {
    PantsFilterInfo info = PantsFilter.parseLine("     [error] /this/is/a/url");
    doTest(new PantsFilterInfo(13,27, "/this/is/a/url", 0), info);
  }
}
