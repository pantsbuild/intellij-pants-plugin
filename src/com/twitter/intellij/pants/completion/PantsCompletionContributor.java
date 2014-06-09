package com.twitter.intellij.pants.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.QualifiedName;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.PyUtil;
import com.jetbrains.python.psi.resolve.CompletionVariantsProcessor;
import com.jetbrains.python.psi.resolve.PyResolveUtil;
import com.jetbrains.python.psi.resolve.ResolveImportUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import com.intellij.codeInsight.lookup.LookupElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * todo: remove dirty hack after PyPreferenceCompletionProvider patch is merged in IntelliJ
 */
public class PantsCompletionContributor extends CompletionContributor {
  public PantsCompletionContributor() {
    extend(
      CompletionType.BASIC,
      psiElement().withParent(PyReferenceExpression.class),
      new CompletionProvider<CompletionParameters>() {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
          final PsiFile psiFile = parameters.getOriginalFile();
          if (!PantsUtil.BUILD.equals(psiFile.getName())) {
            return;
          }
          /*

          TODO: un-hardcode target list
          final PsiElement position = parameters.getPosition();
          List<PsiElement> modules = ResolveImportUtil.resolveModule(
            QualifiedName.fromComponents(PantsUtil.TWITTER, PantsUtil.PANTS),
            psiFile,
            true,
            0
          );
          final CompletionVariantsProcessor processor = new CompletionVariantsProcessor(position);
          for (PsiElement module : modules) {
            module = PyUtil.turnDirIntoInit(module);
            if (module instanceof PyFile) {
              PyResolveUtil.scopeCrawlUp(processor, (PyFile)module, null, null);
            }
          }

          result.addAllElements(processor.getResultList());
          */
          //aliases from TestData/userHome/.pants.d/bin/pants.pex/.deps/pantsbuild.pants-0.0.17-py2-none-any.whl/pants/base/build_file_aliases
          List<LookupElement> aliases = new ArrayList<LookupElement>();
          aliases.add(LookupElementBuilder.create("annotation_processor"));
          aliases.add(LookupElementBuilder.create("artifact"));
          aliases.add(LookupElementBuilder.create("benchmark"));
          aliases.add(LookupElementBuilder.create("bundle"));
          aliases.add(LookupElementBuilder.create("credentials"));
          aliases.add(LookupElementBuilder.create("dependencies"));
          aliases.add(LookupElementBuilder.create("dependencies"));
          aliases.add(LookupElementBuilder.create("egg"));
          aliases.add(LookupElementBuilder.create("exclude"));
          aliases.add(LookupElementBuilder.create("fancy_pants"));
          aliases.add(LookupElementBuilder.create("jar"));
          aliases.add(LookupElementBuilder.create("java_agent"));
          aliases.add(LookupElementBuilder.create("java_library"));
          aliases.add(LookupElementBuilder.create("java_antlr_library"));
          aliases.add(LookupElementBuilder.create("java_protobuf_library"));
          aliases.add(LookupElementBuilder.create("junit_tests"));
          aliases.add(LookupElementBuilder.create("java_tests"));
          aliases.add(LookupElementBuilder.create("java_thrift_library"));
          aliases.add(LookupElementBuilder.create("jvm_binary"));
          aliases.add(LookupElementBuilder.create("jvm_app"));
          aliases.add(LookupElementBuilder.create("page"));
          aliases.add(LookupElementBuilder.create("python_artifact"));
          aliases.add(LookupElementBuilder.create("python_artifact"));
          aliases.add(LookupElementBuilder.create("python_artifact"));
          aliases.add(LookupElementBuilder.create("python_binary"));
          aliases.add(LookupElementBuilder.create("python_library"));
          aliases.add(LookupElementBuilder.create("python_antlr_library"));
          aliases.add(LookupElementBuilder.create("python_requirement"));
          aliases.add(LookupElementBuilder.create("python_thrift_library"));
          aliases.add(LookupElementBuilder.create("python_test"));
          aliases.add(LookupElementBuilder.create("python_test_suite"));
          aliases.add(LookupElementBuilder.create("repo"));
          aliases.add(LookupElementBuilder.create("resources"));
          aliases.add(LookupElementBuilder.create("scala_library"));
          aliases.add(LookupElementBuilder.create("scala_specs"));
          aliases.add(LookupElementBuilder.create("scala_specs"));
          aliases.add(LookupElementBuilder.create("scalac_plugin"));
          aliases.add(LookupElementBuilder.create("source_root"));
          aliases.add(LookupElementBuilder.create("wiki"));

          result.addAllElements(aliases);
        }
      }
    );
  }
}
