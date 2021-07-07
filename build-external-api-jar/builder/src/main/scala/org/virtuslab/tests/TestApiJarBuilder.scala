package org.virtuslab.tests

import java.nio.file.Paths
import org.virtuslab.ideprobe.Extensions.PathExtension
import org.virtuslab.ideprobe.{IdeProbeFixture, WaitLogic}
import scala.concurrent.duration.DurationInt

object TestApiJarBuilder extends IdeProbeFixture {
  val jdkDef = {
    """
      |<application>
      |  <component name="ProjectJdkTable">
      |    <jdk version="2">
      |      <name value="corretto-11" />
      |      <type value="JavaSDK" />
      |      <version value="version 11.0.9" />
      |      <homePath value="/usr/lib/jvm/java-11-openjdk-amd64" />
      |      <roots>
      |        <annotationsPath>
      |          <root type="composite">
      |            <root url="jar://$APPLICATION_HOME_DIR$/plugins/java/lib/jdkAnnotations.jar!/" type="simple" />
      |          </root>
      |        </annotationsPath>
      |        <classPath>
      |          <root type="composite">
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.base" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.compiler" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.datatransfer" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.desktop" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.instrument" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.logging" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.management" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.management.rmi" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.naming" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.net.http" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.prefs" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.rmi" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.scripting" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.se" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.security.jgss" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.security.sasl" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.smartcardio" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.sql" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.sql.rowset" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.transaction.xa" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.xml" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/java.xml.crypto" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.accessibility" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.aot" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.attach" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.charsets" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.compiler" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.crypto.cryptoki" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.crypto.ec" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.dynalink" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.editpad" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.hotspot.agent" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.httpserver" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.internal.ed" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.internal.jvmstat" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.internal.le" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.internal.opt" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.internal.vm.ci" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.internal.vm.compiler" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.internal.vm.compiler.management" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jartool" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.javadoc" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jcmd" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jconsole" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jdeps" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jdi" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jdwp.agent" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jfr" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jlink" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jshell" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jsobject" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.jstatd" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.localedata" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.management" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.management.agent" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.management.jfr" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.naming.dns" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.naming.ldap" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.naming.rmi" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.net" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.pack" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.rmic" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.scripting.nashorn" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.scripting.nashorn.shell" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.sctp" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.security.auth" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.security.jgss" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.unsupported" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.unsupported.desktop" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.xml.dom" type="simple" />
      |            <root url="jrt:///usr/lib/jvm/java-11-openjdk-amd64!/jdk.zipfs" type="simple" />
      |          </root>
      |        </classPath>
      |        <javadocPath>
      |          <root type="composite">
      |            <root url="https://docs.oracle.com/en/java/javase/11/docs/api/" type="simple" />
      |          </root>
      |        </javadocPath>
      |        <sourcePath>
      |          <root type="composite" />
      |        </sourcePath>
      |      </roots>
      |      <additional />
      |    </jdk>
      |  </component>
      |</application>
      """.stripMargin
  }
  val artifactVal =
    """
      |<component name="ArtifactManager">
      |  <artifact type="jar" name="external-system-test-api">
      |    <output-path>$PROJECT_DIR$/out/artifacts/external_system_test_api</output-path>
      |    <root id="archive" name="external-system-test-api.jar">
      |      <element id="module-test-output" name="intellij.platform.externalSystem.impl" />
      |      <element id="module-test-output" name="intellij.platform.externalSystem.tests" />
      |      <element id="module-output" name="intellij.platform.externalSystem.api" />
      |      <element id="module-output" name="intellij.platform.externalSystem.impl" />
      |    </root>
      |  </artifact>
      |
      |
      |</component>""".stripMargin

  def main(args: Array[String]): Unit = {
    val fixture = fixtureFromConfig()
      .withAfterIntelliJInstall { (_, installedIntelliJ) =>
        installedIntelliJ.paths.config.resolve("options/jdk.table.xml").write(jdkDef)
      }.withAfterWorkspaceSetup { (_, path) =>
        path.resolve(".idea/artifacts/external_system_test_api.xml").write(artifactVal)
      }

    fixture.run { intelliJ =>
      val project = intelliJ.probe.openProject(intelliJ.workspace, WaitLogic.emptyNamedBackgroundTasks(atMost = 1.hour))
      intelliJ.probe.buildArtifact(project, "external-system-test-api")
      intelliJ.probe.await(WaitLogic.backgroundTaskCompletes("Build", maxTaskDuration = 30.minutes))

      val outputPath = "out/artifacts/external_system_test_api/external-system-test-api.jar"
      val artifact = intelliJ.workspace.resolve(outputPath)
      artifact.copyTo(Paths.get("/tmp/external-system-test-api.jar"))
    }
  }
}
