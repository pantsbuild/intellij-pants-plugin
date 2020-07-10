package com.twitter.intellij.pants

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@RunWith(classOf[Suite])
@SuiteClasses(Array(
  classOf[OpenProjectTestBsp],
  classOf[OpenProjectTestPants],
  classOf[BUILDFilesTest],
  classOf[PantsSettingsTest],
  classOf[ThriftIdeaPluginTest]
))
class Suite1


@RunWith(classOf[Suite])
@SuiteClasses(Array(
  classOf[RunAppTest],
  classOf[RunTestsTest]
))
class Suite2
