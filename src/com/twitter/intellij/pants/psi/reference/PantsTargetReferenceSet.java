package com.twitter.intellij.pants.psi.reference;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PantsTargetReferenceSet extends FileReferenceSet {

  public static final Pattern DELIMITERS = Pattern.compile("\\\\|/");
  private final PyStringLiteralExpression myStringLiteralExpression;


  public PantsTargetReferenceSet(@NotNull PyStringLiteralExpression element) {
    this(element, SystemInfo.isFileSystemCaseSensitive);
  }

  public PantsTargetReferenceSet(@NotNull PyStringLiteralExpression element, boolean caseSensitive) {
    this(element.getStringValue(), element, element.getStringValueTextRange().getStartOffset(), null, caseSensitive, true, new FileType[0]);
  }

  public PantsTargetReferenceSet(@NotNull String str,
                                 @NotNull PyStringLiteralExpression element,
                                 int startInElement,
                                 PsiReferenceProvider provider,
                                 boolean caseSensitive, boolean endingSlashNotAllowed, @Nullable FileType[] suitableFileTypes) {
    super(str, element, startInElement, provider, caseSensitive, endingSlashNotAllowed,
      suitableFileTypes);
    myStringLiteralExpression = element;
    reparse();
  }


  @Override
  protected void reparse() {
    //noinspection ConstantConditions
    if (myStringLiteralExpression != null) {
      final List<FileReference> references = getFileReferences(myStringLiteralExpression);
      myReferences = references.toArray(new FileReference[references.size()]);
    }
  }

  @Override
  public boolean isAbsolutePathReference() {
    return true;
  }

  @NotNull
  private List<FileReference> getFileReferences(@NotNull PyStringLiteralExpression expression) {
    final String[] pathAndTarget = expression.getStringValue().split(":");

    final String path = pathAndTarget[0];
    final String target = pathAndTarget.length > 1 ? pathAndTarget[1] : null;

    final Matcher matcher = DELIMITERS.matcher(path);
    int start = 0;
    int index = 0;
    final List<FileReference> results = new ArrayList<FileReference>();
    while (matcher.find()) {
      final String s = path.substring(start, matcher.start());
      if (!s.isEmpty()) {
        final TextRange range = TextRange.create(expression.valueOffsetToTextOffset(start),
          expression.valueOffsetToTextOffset(matcher.start()));
        results.add(createFileReference(range, index++, s));
      }
      start = matcher.end();
    }
    final String s = path.substring(start);
    if (!s.isEmpty()) {
      final TextRange range = TextRange.create(expression.valueOffsetToTextOffset(start),
        expression.valueOffsetToTextOffset(path.length()));
      results.add(createFileReference(range, index, s));
    }

    // todo: add target name ref

    return results;
  }
}
