package com.twitter.intellij.pants.probe.handlers

import java.nio.file.Path
import com.intellij.openapi.externalSystem.service.project.wizard.SelectExternalProjectStep
import com.intellij.projectImport.ImportChooserStep
import com.intellij.projectImport.ProjectImportProvider
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBRadioButton
import com.twitter.intellij.pants.protocol.PantsProjectSettingsChangeRequest
import com.twitter.intellij.pants.service.project.wizard.PantsProjectImportProvider
import com.twitter.intellij.pants.settings.ImportFromPantsControl
import com.twitter.intellij.pants.settings.PantsProjectSettingsControl
import com.twitter.handlers.IntelliJApi
import com.twitter.handlers.Projects
import com.twitter.ideprobe.protocol.ProjectRef
import com.twitter.ideprobe.protocol.Setting

object PantsImport extends IntelliJApi {

  // TODO update after merge of https://github.com/pantsbuild/intellij-pants-plugin/pull/479
  // It removes the dialog for pants import depth and moves it to settings. Currently
  // it is not possible to complete incremental import scenario as we can't interact
  // with the dialog.
  def importProject(path: Path, settings: PantsProjectSettingsChangeRequest): ProjectRef = {
    Projects.importFromSources(
      path, {
        case step: ImportChooserStep =>
          selectPantsImportModel(step)
        case step: SelectExternalProjectStep =>
          configurePantsSettings(step, settings)
      }
    )
  }

  private def selectPantsImportModel(step: ImportChooserStep): Unit = {
    val importCheckbox = step.field[JBRadioButton]("importFrom")
    importCheckbox.setSelected(true)

    val providersList = step.field[JBList[ProjectImportProvider]]("list")
    val pants = providersList.items
      .collectFirst { case p: PantsProjectImportProvider => p }
      .getOrElse(error(s"Could not find pants import provider. Available providers are ${providersList.items}"))
    providersList.setSelectedValue(pants, false)
  }

  private def configurePantsSettings(
      step: SelectExternalProjectStep,
      settings: PantsProjectSettingsChangeRequest
  ): Unit = {
    val importFromPantsControl = step.field[ImportFromPantsControl]("control")
    val pantsProjectSettingsControl =
      importFromPantsControl.field[PantsProjectSettingsControl]("projectSettingsControl")

    def checkBox(setting: Setting[Boolean], checkBoxName: String): Unit = {
      setting.foreach { value =>
        val cb = pantsProjectSettingsControl.field[JBCheckBox](checkBoxName)
        cb.setSelected(value)
      }
    }

    def checkBoxList(setting: Setting[Seq[String]], listName: String): Unit = {
      setting.foreach { toCheck =>
        val list = pantsProjectSettingsControl.field[CheckBoxList[String]](listName)
        list.items.foreach { item =>
          list.setItemSelected(item, toCheck.contains(item))
        }
      }
    }

    checkBox(settings.incrementalProjectImport, "enableIncrementalImportCheckBox")
    checkBox(settings.loadSourcesAndDocsForLibs, "libsWithSourcesCheckBox")
    checkBox(settings.useIdeaProjectJdk, "useIdeaProjectJdkCheckBox")
    checkBox(settings.importSourceDepsAsJars, "importSourceDepsAsJarsCheckBox")
    checkBox(settings.useIntellijCompiler, "useIntellijCompilerCheckBox")
    checkBoxList(settings.selectedTargets, "targetSpecsBox")
  }

}
