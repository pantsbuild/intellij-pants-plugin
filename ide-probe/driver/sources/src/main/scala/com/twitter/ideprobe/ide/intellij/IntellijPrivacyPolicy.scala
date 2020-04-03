package com.twitter.ideprobe.ide.intellij

import java.nio.file.Path
import com.twitter.ideprobe.Extensions._

object IntellijPrivacyPolicy {
  def installAgreementIn(dir: Path): Unit = {
    dir
      .resolve(s".java/.userPrefs/jetbrains/$policyNode/prefs.xml")
      .write(agreementContent)
  }

  private val agreementContent = s"""|<?xml version="1.0" encoding="UTF-8" standalone="no"?>
                                      |<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">
                                      |<map MAP_XML_VERSION="1.0">
                                      |  <entry key="accepted_version" value="2.1"/>
                                      |  <entry key="eua_accepted_version" value="1.1"/>
                                      |  <entry key="privacyeap_accepted_version" value="2.1"/>
                                      |</map>
                                      |""".stripMargin

  private val policyNode = {
    val value = Class.forName("java.util.prefs.FileSystemPreferences")
    val dirName = value.getDeclaredMethod("dirName", classOf[String])
    dirName.setAccessible(true)
    try {
      dirName.invoke(null, "privacy_policy").asInstanceOf[String]
    } finally {
      dirName.setAccessible(false)
    }
  }
}
