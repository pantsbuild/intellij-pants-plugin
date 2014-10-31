// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.unquoteString;

public class PantsPsiUtil {

  public static List<Target> findTargets(@NotNull PsiFile file) {
    final List<Target> targets = new ArrayList<Target>();
    for (PyExpressionStatement statement : PsiTreeUtil.findChildrenOfType(file, PyExpressionStatement.class)) {
      Target target = findTarget(statement);
      if (target != null) {
        targets.add(target);
      }
    }
    return targets;
  }

  @Nullable
  public static Target findTarget(@NotNull PyExpressionStatement statement) {
    for (PyCallExpression expression : PsiTreeUtil.findChildrenOfType(statement, PyCallExpression.class)) {
      for (PyArgumentList args : PsiTreeUtil.findChildrenOfType(expression, PyArgumentList.class)) {
        final PyKeywordArgument arg = args.getKeywordArgument("name");
        final PyExpression valueExpression = arg != null ? arg.getValueExpression() : null;
        if (valueExpression != null) {
          return new Target(unquoteString(valueExpression.getText()), expression.getFirstChild().getText());
        }
      }
    }
    return null;
  }

  @NotNull
  public static Map<String, PyReferenceExpression> findTargetDefinitions(@NotNull PyFile pyFile) {
    final PyFunction buildFileAliases = pyFile.findTopLevelFunction("build_file_aliases");
    final PyStatement[] statements = buildFileAliases != null ? buildFileAliases.getStatementList().getStatements() : PyStatement.EMPTY_ARRAY;
    final Map<String, PyReferenceExpression> result = new HashMap<String, PyReferenceExpression>();
    for (PyStatement statement : statements) {
      if (!(statement instanceof PyReturnStatement)) {
        continue;
      }
      final PyExpression returnExpression = ((PyReturnStatement)statement).getExpression();
      if (!(returnExpression instanceof PyCallExpression)) {
        continue;
      }
      final PyArgumentList argumentList = ((PyCallExpression)returnExpression).getArgumentList();
      final Collection<PyKeywordArgument> targetDefinitions = PsiTreeUtil.findChildrenOfType(argumentList, PyKeywordArgument.class);
      for (PyKeywordArgument targets : targetDefinitions) {
        final PyExpression targetsExpression = targets != null ? targets.getValueExpression() : null;
        if (targetsExpression instanceof PyDictLiteralExpression) {
          for (PyKeyValueExpression keyValueExpression : ((PyDictLiteralExpression)targetsExpression).getElements()) {
            final PyExpression keyExpression = keyValueExpression.getKey();
            final PyExpression valueExpression = keyValueExpression.getValue();
            if (keyExpression instanceof PyStringLiteralExpression) {
              result.put(
                ((PyStringLiteralExpression)keyExpression).getStringValue(),
                valueExpression instanceof PyReferenceExpression ? (PyReferenceExpression)valueExpression : null
              );
            }
          }
        }
      }
    }
    return result;
  }
}