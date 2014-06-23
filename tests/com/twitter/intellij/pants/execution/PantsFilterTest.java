package com.twitter.intellij.pants.execution;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.execution.PantsFilter.PantsFilterInfo;
import junit.framework.TestCase;


/**
 * Created by ajohnson on 6/19/14.
 */
public class PantsFilterTest extends TestCase {//LightCodeInsightFixtureTestCase {

  public void doTest(PantsFilterInfo expected, PantsFilterInfo actual) {
    if (expected == null) {
      assertEquals(null, actual);
    } else {
      assertNotNull(actual);
      assertEquals(expected.getStart(), actual.getStart());
      assertEquals(expected.getEnd(), actual.getEnd());
      assertEquals(expected.getFilePath(), actual.getFilePath());
      assertEquals(expected.getLineNumber(), actual.getLineNumber());
    }
  }

  public void testUrl() {
    PantsFilterInfo info = PantsFilter.parseLine("/this/is/a/url");
    doTest(new PantsFilterInfo(0,14,"/this/is/a/url",0), info);
  }

  public void testUrlWithSpaces() {
    PantsFilterInfo info = PantsFilter.parseLine("     /this/is/a/url");
    doTest(new PantsFilterInfo(5,19,"/this/is/a/url",0), info);
  }

  public void testUrlWithLineNumber() {
    PantsFilterInfo info = PantsFilter.parseLine("/this/is/a/url:23");
    doTest(new PantsFilterInfo(0,17,"/this/is/a/url",22), info);
  }

  public void testUrlWithLineNumberAndMessage() {
    PantsFilterInfo info = PantsFilter.parseLine("/this/is/a/url:23: error: ...");
    doTest(new PantsFilterInfo(0,17,"/this/is/a/url",22), info);
  }
}
