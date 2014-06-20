package com.twitter.intellij.pants.execution;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.base.PantsCodeInsightFixtureTestCase;
import com.intellij.execution.filters.Filter.Result;
import com.intellij.execution.filters.Filter.ResultItem;

/**
 * Created by ajohnson on 6/19/14.
 */
public class PantsFilterTest extends PantsCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "execution";
  }

  public ResultItem setUpTest(String line) {
    Project project = myFixture.getProject();
    myFixture.addFileToProject("filterTest", "file text");
    final VirtualFile testFile = myFixture.copyFileToProject("filterTest", "testPath");
    myFixture.configureFromExistingVirtualFile(testFile);
    PantsFilter filter = new PantsFilter(project);
    Result result =  filter.applyFilter(line, line.length());
    return result == null ? null : result.getResultItems().get(0);
  }

  public void testFilterBasic() {
    ResultItem result = setUpTest("   blah blah output");
    assertEquals(null, result);
  }
  public void testUrlWithNoSpaceAtEnd() {
    ResultItem result = setUpTest("/Users/ajohnson/workspace/intellij-pants/testData/execution/filterTest");
    assertEquals(0, result.highlightStartOffset);
    assertEquals(70, result.highlightEndOffset);

  }
  public void testUrlWithLineNumber() {
    ResultItem result = setUpTest("/Users/ajohnson/workspace/intellij-pants/testData/execution/filterTest:20:error");
    assertEquals(0, result.highlightStartOffset);
    assertEquals(73, result.highlightEndOffset);
  }

}
