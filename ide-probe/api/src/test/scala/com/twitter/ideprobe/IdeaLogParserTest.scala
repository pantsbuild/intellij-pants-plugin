package com.twitter.ideprobe

import com.twitter.ideprobe.log.IdeaLogParser
import org.junit.Assert
import org.junit.Test
import scala.util.Random

class IdeaLogParserTest extends Assertions {

  @Test def extractErrorsFromActualLogFile(): Unit = {
    val logs =
      """2020-01-20 15:47:24,547 [      0]   INFO -        #com.intellij.idea.Main - ------------------------------------------------------ IDE STARTED ------------------------------------------------------ 
        |2020-01-20 15:47:24,563 [     16]   INFO - ntellij.idea.ApplicationLoader - CPU cores: 8; ForkJoinPool.commonPool: java.util.concurrent.ForkJoinPool@1e1b1f9a[Running, parallelism = 8, size = 0, active = 0, running = 0, steals = 0, tasks = 0, submissions = 0]; factory: com.intellij.concurrency.IdeaForkJoinWorkerThreadFactory@2a9315ca 
        |2020-01-20 15:47:24,573 [     26]   INFO -        #com.intellij.idea.Main - JNA library (64-bit) loaded in 21 ms 
        |2020-01-20 15:47:24,630 [     83]   INFO -        #com.intellij.idea.Main - IDE: IntelliJ IDEA (build #IC-193.5233.102, 28 Nov 2019 00:47) 
        |2020-01-20 15:47:24,630 [     83]   INFO -        #com.intellij.idea.Main - OS: Linux (5.0.0-37-generic, amd64) 
        |2020-01-20 15:47:24,630 [     83]   INFO -        #com.intellij.idea.Main - JRE: 1.8.0_232-8u232-b09-0ubuntu1~18.04.1-b09 (Private Build) 
        |2020-01-20 15:47:24,630 [     83]   INFO -        #com.intellij.idea.Main - JVM: 25.232-b09 (OpenJDK 64-Bit Server VM) 
        |2020-01-20 15:47:24,630 [     83]   INFO -        #com.intellij.idea.Main - JVM Args: -Djava.awt.headless=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -XX:ErrorFile=/home/lukasz/java_error_in_IDEA_%p.log -XX:HeapDumpPath=/home/lukasz/java_error_in_IDEA.hprof -Didea.paths.selector=IdeaIC2019.3 -Djb.vmOptionsFile=/tmp/intellij-instance-193.5233.102-1762822693100025663/bin/ideprobe.vmoptions -Didea.properties.file=/tmp/intellij-instance-193.5233.102-1762822693100025663/bin/idea.properties -Didea.platform.prefix=Idea -Didea.jre.check=true 
        |2020-01-20 15:47:24,631 [     84]   INFO -        #com.intellij.idea.Main - ext: /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext: [java-atk-wrapper.jar, sunpkcs11.jar, dnsns.jar, nashorn.jar, jaccess.jar, localedata.jar, sunec.jar, cldrdata.jar, icedtea-sound.jar, zipfs.jar, libatk-wrapper.so, sunjce_provider.jar] 
        |2020-01-20 15:47:24,631 [     84]   INFO -        #com.intellij.idea.Main - charsets: JNU=UTF-8 file=UTF-8 
        |2020-01-20 15:47:24,898 [    351]   INFO - llij.ide.plugins.PluginManager - Plugin "Groovy" misses optional descriptor duplicates-groovy.xml 
        |2020-01-20 15:47:24,899 [    352]   INFO - llij.ide.plugins.PluginManager - Plugin "Groovy" misses optional descriptor duplicates-detection-groovy.xml 
        |2020-01-20 15:47:24,958 [    411]   INFO - llij.ide.plugins.PluginManager - Plugin "Java" misses optional descriptor profiler-java.xml 
        |2020-01-20 15:47:24,996 [    449]   INFO - llij.ide.plugins.PluginManager - Plugin "Groovy" misses optional descriptor duplicates-groovy.xml 
        |2020-01-20 15:47:24,996 [    449]   INFO - llij.ide.plugins.PluginManager - Plugin "Groovy" misses optional descriptor duplicates-detection-groovy.xml 
        |2020-01-20 15:47:25,053 [    506]   WARN - llij.ide.plugins.PluginManager - Plugin "Scala" is incompatible (until build 193.0 < IC-193.5233.102) 
        |2020-01-20 15:47:25,053 [    506]   WARN - llij.ide.plugins.PluginManager - Plugin "Scala" is incompatible (until build 193.0 < IC-193.5233.102) 
        |2020-01-20 15:47:25,100 [    553]   INFO - llij.ide.plugins.PluginManager - Plugin "Java" misses optional descriptor profiler-java.xml 
        |2020-01-20 15:47:25,201 [    654]  ERROR - llij.ide.plugins.PluginManager - Problems found loading plugins:
        |Plugin "Scala" is incompatible (target build range is 192.5118.30 to 193.0) 
        |java.lang.Throwable: Problems found loading plugins:
        |Plugin "Scala" is incompatible (target build range is 192.5118.30 to 193.0)
        |	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:145)
        |	at com.intellij.ide.plugins.PluginManagerCore.prepareLoadingPluginsErrorMessage(PluginManagerCore.java:635)
        |	at com.intellij.ide.plugins.PluginManagerCore.prepareLoadingPluginsErrorMessage(PluginManagerCore.java:1030)
        |	at com.intellij.ide.plugins.PluginManagerCore.initializePlugins(PluginManagerCore.java:1434)
        |	at com.intellij.ide.plugins.PluginManagerCore.initPlugins(PluginManagerCore.java:1599)
        |	at com.intellij.ide.plugins.PluginManagerCore.getLoadedPlugins(PluginManagerCore.java:149)
        |	at com.intellij.idea.ApplicationLoader.initApplication(ApplicationLoader.kt:375)
        |	at com.intellij.idea.MainImpl.start(MainImpl.java:19)
        |	at com.intellij.idea.StartupUtil.lambda$startApp$5(StartupUtil.java:248)
        |	at com.intellij.util.ui.EdtInvocationManager.executeWithCustomManager(EdtInvocationManager.java:73)
        |	at com.intellij.idea.StartupUtil.startApp(StartupUtil.java:243)
        |	at com.intellij.idea.StartupUtil.prepareApp(StartupUtil.java:215)
        |	at com.intellij.ide.plugins.MainRunner.lambda$start$0(MainRunner.java:39)
        |	at java.lang.Thread.run(Thread.java:748)
        |2020-01-20 15:47:25,203 [    656]  ERROR - llij.ide.plugins.PluginManager - IntelliJ IDEA 2019.3  Build #IC-193.5233.102 
        |2020-01-20 15:47:25,203 [    656]  ERROR - llij.ide.plugins.PluginManager - JDK: 1.8.0_232; VM: OpenJDK 64-Bit Server VM; Vendor: Private Build 
        |2020-01-20 15:47:25,204 [    657]  ERROR - llij.ide.plugins.PluginManager - OS: Linux 
        |2020-01-20 15:47:25,241 [    694]   INFO - llij.ide.plugins.PluginManager - Loaded bundled plugins: IDEA CORE (193.5233.102) 
        |2020-01-20 15:47:25,241 [    694]   INFO - llij.ide.plugins.PluginManager - Loaded custom plugins: Android Support (10.3.5), Ant (193.5233.102), Bytecode Viewer (193.5233.102), ChangeReminder (193.5233.102), Configuration Script (193.5233.102), Copyright (193.5233.102), Coverage (193.5233.102), Eclipse Interoperability (193.5233.102), EditorConfig (193.5233.102), Git (193.5233.102), GitHub (193.5233.102), Gradle (193.5233.102), Gradle-Java (193.5233.102), Gradle-Maven (193.5233.102), Groovy (193.5233.102), IntelliLang (193.5233.102), JUnit (193.5233.102), Java (193.5233.102), Java Bytecode Decompiler (193.5233.102), Java IDE Customization (193.5233.102), Java Internationalization (193.5233.102), Java Stream Debugger (193.5233.102), JavaFX (193.5233.102), Kotlin (1.3.60-release-IJ2019.3-1), Machine Learning Code Completion (193.5233.102), Markdown (193.5233.102), Maven (193.5233.102), Mercurial (193.5233.102), Plugin DevKit (193.5233.102), Properties (193.5233.102), Settings Repository (193.5233.102), Shell Script (193.5233.102), Smali Support (193.5233.102), Subversion (193.5233.102), Task Management (193.5233.102), Terminal (193.5233.102), TestNG (193.5233.102), TextMate bundles (193.5233.102), UI Designer (193.5233.102), Twitter IdeProbe (193.5233.102), XPathView + XSLT (193.5233.102), XSLT Debugger (193.5233.102), YAML (193.5233.102) 
        |2020-01-20 15:47:25,241 [    694]   INFO - llij.ide.plugins.PluginManager - Disabled plugins: Scala (2019.2.37) 
        |2020-01-20 15:47:25,746 [   1199]   INFO - com.intellij.util.ui.JBUIScale - System scale factor: 1.0 (IDE-managed HiDPI) 
        |2020-01-20 15:47:25,757 [   1210]   INFO - fs.newvfs.persistent.FSRecords - Marking VFS as corrupted: '/tmp/intellij-instance-193.5233.102-1762822693100025663/system/caches/names.dat' does not exist 
        |2020-01-20 15:47:25,767 [   1220]   INFO - ellij.util.io.PagedFileStorage - lower=100; upper=500; buffer=10; max=5274 
        |2020-01-20 15:47:25,836 [   1289]   INFO - rains.ide.BuiltInServerManager - built-in server started, port 63343 
        |2020-01-20 15:47:25,847 [   1300]   INFO - pl.local.NativeFileWatcherImpl - Native file watcher is disabled 
        |2020-01-20 15:47:25,993 [   1446]   INFO - til.net.ssl.CertificateManager - Default SSL context initialized 
        |2020-01-20 15:47:26,240 [   1693]   INFO - ij.psi.stubs.StubUpdatingIndex - Following new file types will be indexed:CLASS,KJSM,KNM,Groovy,kotlin_builtins,Markdown,Properties,DGM,XML,JAVA,HTML,Kotlin 
        |2020-01-20 15:47:26,446 [   1899]   INFO - il.indexing.FileBasedIndexImpl - Indices to be built:FrameworkDetectionIndex,TodoIndex,IdIndex,FilenameIndex,filetypes,Stubs,Trigram.Index,fileIncludes,DomFileIndex,RelaxSymbolIndex,XmlTagNames,XmlNamespaces,html5.custom.attributes.index,SchemaTypeInheritance,json.file.root.values,ImageFileInfoIndex,bytecodeAnalysis,java.auto.module.name,java.null.method.argument,java.simple.property,java.fun.expression,java.binary.plus.expression,RefQueueIndex,xmlProperties,ant-imports,XsltSymbolIndex,GroovyDslFileIndex,groovy.trait.fields,groovy.trait.methods,FormClassIndex,IdeaPluginRegistrationIndex,PluginIdModuleIndex,PluginIdDependenciesIndex,devkit.ExtensionPointIndex,editorconfig.index.name,org.jetbrains.kotlin.idea.versions.KotlinJvmMetadataVersionIndex,org.jetbrains.kotlin.idea.versions.KotlinJsMetadataVersionIndex,org.jetbrains.kotlin.idea.vfilefinder.KotlinClassFileIndex,org.jetbrains.kotlin.idea.vfilefinder.KotlinJavaScriptMetaFileIndex,org.jetbrains.kotlin.idea.vfilefinder.KotlinMetadataFileIndex,org.jetbrains.kotlin.idea.vfilefinder.KotlinMetadataFilePackageIndex,org.jetbrains.kotlin.idea.vfilefinder.KotlinModuleMappingIndex,org.jetbrains.kotlin.idea.vfilefinder.KotlinPackageSourcesMemberNamesIndex,org.jetbrains.kotlin.idea.vfilefinder.KotlinJvmModuleAnnotationsIndex,org.jetbrains.kotlin.ide.konan.index.KotlinNativeMetaFileIndex,DataBindingXmlIndex,JavaFxControllerClassIndex,javafx.id.name,javafx.custom.component,yaml.keys.name 
        |2020-01-20 15:47:26,448 [   1901]   INFO - pl$FileIndexDataInitialization - Initialization done: 665 
        |2020-01-20 15:47:26,549 [   2002]   INFO - tellij.psi.stubs.StubIndexImpl - Following stub indices will be built:dom.namespaceKey,dom.elementClass,java.annotations,java.class.extlist,java.field.name,java.method.name,jvm.static.member.name,jvm.static.member.type,java.anonymous.baseref,java.method.parameter.types,java.class.shortname,java.class.fqn,java.module.name,properties.index,gr.class.fqn,gr.script.fqn,gr.field.name,gr.method.name,gr.annot.method.name,gr.annot.members,gr.script.class,gr.class.super,gr.anonymous.class,markdown.header,org.jetbrains.kotlin.idea.stubindex.KotlinExactPackagesIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelClassByPackageIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionByPackageIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyByPackageIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelTypeAliasByPackageIndex,org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinPropertyShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinSuperClassIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyFqnNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelTypeAliasFqNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelExtensionsByReceiverTypeIndex,org.jetbrains.kotlin.idea.stubindex.KotlinAnnotationsIndex,org.jetbrains.kotlin.idea.stubindex.KotlinProbablyNothingFunctionShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinProbablyNothingPropertyShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinProbablyContractedFunctionShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinFileFacadeFqNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinFilePartClassIndex,org.jetbrains.kotlin.idea.stubindex.KotlinFileFacadeClassByPackageIndex,org.jetbrains.kotlin.idea.stubindex.KotlinFileFacadeShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinMultifileClassPartIndex,org.jetbrains.kotlin.idea.stubindex.KotlinScriptFqnIndex,org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasByExpansionShortNameIndex,org.jetbrains.kotlin.idea.stubindex.KotlinOverridableInternalMembersShortNameIndex 
        |2020-01-20 15:47:26,549 [   2002]   INFO - exImpl$StubIndexInitialization - Initialization done: 100 
        |""".stripMargin

    val expected =
      """Problems found loading plugins:
        |Plugin "Scala" is incompatible (target build range is 192.5118.30 to 193.0) 
        |java.lang.Throwable: Problems found loading plugins:
        |Plugin "Scala" is incompatible (target build range is 192.5118.30 to 193.0)
        |	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:145)
        |	at com.intellij.ide.plugins.PluginManagerCore.prepareLoadingPluginsErrorMessage(PluginManagerCore.java:635)
        |	at com.intellij.ide.plugins.PluginManagerCore.prepareLoadingPluginsErrorMessage(PluginManagerCore.java:1030)
        |	at com.intellij.ide.plugins.PluginManagerCore.initializePlugins(PluginManagerCore.java:1434)
        |	at com.intellij.ide.plugins.PluginManagerCore.initPlugins(PluginManagerCore.java:1599)
        |	at com.intellij.ide.plugins.PluginManagerCore.getLoadedPlugins(PluginManagerCore.java:149)
        |	at com.intellij.idea.ApplicationLoader.initApplication(ApplicationLoader.kt:375)
        |	at com.intellij.idea.MainImpl.start(MainImpl.java:19)
        |	at com.intellij.idea.StartupUtil.lambda$startApp$5(StartupUtil.java:248)
        |	at com.intellij.util.ui.EdtInvocationManager.executeWithCustomManager(EdtInvocationManager.java:73)
        |	at com.intellij.idea.StartupUtil.startApp(StartupUtil.java:243)
        |	at com.intellij.idea.StartupUtil.prepareApp(StartupUtil.java:215)
        |	at com.intellij.ide.plugins.MainRunner.lambda$start$0(MainRunner.java:39)
        |	at java.lang.Thread.run(Thread.java:748)
        |IntelliJ IDEA 2019.3  Build #IC-193.5233.102 
        |JDK: 1.8.0_232; VM: OpenJDK 64-Bit Server VM; Vendor: Private Build 
        |OS: Linux """.stripMargin

    val list = IdeaLogParser.extractErrors(logs)
    Assert.assertEquals(Seq(expected), list)
  }

  @Test def extractMultipleErrors(): Unit = {
    def logLine(level: String = "INFO", message: String = Random.alphanumeric.take(10).mkString): String = {
      s"2020-01-20 15:47:25,203 [    656]  $level - class.f.q.n - $message"
    }

    val logs = Seq(
      logLine(),
      logLine(),
      logLine(level = "ERROR", "error1-line1"),
      logLine(level = "ERROR", "error1-line2"),
      logLine(),
      logLine(level = "ERROR", "error2-line1"),
      "error2-message-line-2"
    ).mkString("\n")

    val expected = Seq(
      "error1-line1\nerror1-line2",
      "error2-line1\nerror2-message-line-2"
    )

    val list = IdeaLogParser.extractErrors(logs)
    Assert.assertEquals(expected, list)
  }

}
