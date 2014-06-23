package com.twitter.intellij.pants.execution;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.execution.PantsFilter.PantsFilterInfo;


/**
 * Created by ajohnson on 6/19/14. 
 */
public class PantsFilterTest extends LightCodeInsightFixtureTestCase {

  public void testBasic() {
    PantsFilterInfo info = PantsFilter.parseLine("text text text");
    assertEquals(null, info);
  }

  public void testUrl() {
    PantsFilterInfo info = PantsFilter.parseLine("/this/is/a/url");
    assertEquals(null, info);
  }

  public void testValidUrl() {
    PantsFilterInfo info = PantsFilter.parseLine(this.getTestDataPath());
    assertNotNull(info);
    assertEquals(0, info.getStart());
    assertEquals(this.getTestDataPath().length(), info.getEnd());
  }

  public void testValidUrlWithSpaces() {
    PantsFilterInfo info = PantsFilter.parseLine("     " + this.getTestDataPath());
    assertNotNull(info);
    assertEquals(5, info.getStart());
    assertEquals(this.getTestDataPath().length() + 5, info.getEnd());
  }

  public void testUrlWithLineNumber() {
    PantsFilterInfo info = PantsFilter.parseLine(this.getTestDataPath() + ":23");
    assertNotNull(info);
    assertEquals(0, info.getStart());
    assertEquals(this.getTestDataPath().length() + 3, info.getEnd());
    assertEquals(22, info.getLineNumber());
  }

  public void testUrlWithLineNumberAndMessage() {
    PantsFilterInfo info = PantsFilter.parseLine(this.getTestDataPath() + ":23: error: ...");
    assertNotNull(info);
    assertEquals(0, info.getStart());
    assertEquals(this.getTestDataPath().length() + 3, info.getEnd());
    assertEquals(22, info.getLineNumber());
  }
}
