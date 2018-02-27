package org.pantsbuild.testproject.cp_print;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class AppTest {

  @Test
  public void testClasspath() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();

    URL[] urls = ((URLClassLoader) cl).getURLs();

    Set<String> jarNames = Arrays.stream(urls).map(u -> new File(u.getPath()).getName()).collect(Collectors.toSet());

    assertTrue(jarNames.contains("manifest.jar"));

    // autovalue imported into the project should not be directly be on the classpath
    assertFalse(jarNames.stream().anyMatch(s -> s.contains("auto-value")));
  }
}