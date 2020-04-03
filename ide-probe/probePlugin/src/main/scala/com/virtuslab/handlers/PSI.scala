package com.twitter.handlers

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.twitter.ideprobe.protocol.FileRef
import com.twitter.ideprobe.protocol.ProjectRef
import com.twitter.ideprobe.protocol.Reference
import scala.collection.mutable

object PSI extends IntelliJApi {
  def resolve(ref: FileRef): PsiFile = {
    val project = Projects.resolve(ref.project)
    val file = VFS.resolve(ref)
    PsiManager.getInstance(project).findFile(file)
  }

  def references(file: FileRef): Seq[Reference] = {
    references(resolve(file))
  }

  def references(root: PsiElement): Seq[Reference] = {
    val references = mutable.Buffer[Reference]()
    val elements = mutable.Stack[PsiElement](root)
    while (elements.nonEmpty) {
      val element = elements.pop()
      elements.pushAll(element.getChildren)
      element.getReferences.foreach { reference =>
        val text = reference.getCanonicalText
        Option(reference.resolve())
          .flatMap(toTarget)
          .map(Reference(text, _))
          .foreach(references.append)
      }
    }

    references.toSet.toList
  }

  private def toTarget(element: PsiElement): Option[Reference.Target] = {
    Option(element.getProject).map(_.getName).map(ProjectRef(_)).flatMap { project =>
      element match {
        case file: PsiFile =>
          val path = file.getVirtualFile.getPath
          val ref = Reference.Target.File(FileRef(project, path))
          Some(ref)
        case _ =>
          None
      }
    }
  }
}
