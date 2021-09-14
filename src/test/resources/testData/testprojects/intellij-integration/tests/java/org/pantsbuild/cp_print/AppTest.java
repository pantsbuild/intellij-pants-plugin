package org.pantsbuild.testproject.cp_print;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class AppTest {

  @Test
  public void testClasspath() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();

    URL[] urls = ((URLClassLoader) cl).getURLs();

    Set<String> jarNames = Arrays.stream(urls)
      .map(u -> new File(u.getPath()).getName()).collect(Collectors.toSet());

    assertTrue(jarNames.toString(), jarNames.contains("manifest.jar"));

    // autovalue imported into the project should not be directly be on the classpath
    assertFalse(jarNames.toString(), jarNames.contains("guava-23.0.jar"));

    // Make sure guava is accessible, so it is not coming from "guava-23.0.jar"
    // directly on the classpath, but from Pants' manifest.jar
    HashSet<Object> objects = Sets.newHashSet();
  }
}