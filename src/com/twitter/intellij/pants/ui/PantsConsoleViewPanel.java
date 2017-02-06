// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).
package com.twitter.intellij.pants.ui;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.CopyProvider;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.OccurenceNavigatorSupport;
import com.intellij.ide.actions.CloseTabToolbarAction;
import com.intellij.ide.errorTreeView.ErrorTreeElement;
import com.intellij.ide.errorTreeView.ErrorTreeElementKind;
import com.intellij.ide.errorTreeView.ErrorTreeNodeDescriptor;
import com.intellij.ide.errorTreeView.ErrorViewStructure;
import com.intellij.ide.errorTreeView.ErrorViewTreeBuilder;
import com.intellij.ide.errorTreeView.HotfixData;
import com.intellij.ide.errorTreeView.NavigatableMessageElement;
import com.intellij.ide.errorTreeView.NewErrorTreeRenderer;
import com.intellij.ide.errorTreeView.SimpleErrorData;
import com.intellij.ide.errorTreeView.impl.ErrorTreeViewConfiguration;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.MessageView;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Alarm;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.MutableErrorTreeView;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

public class PantsConsoleViewPanel extends JPanel implements OccurenceNavigator, MutableErrorTreeView, CopyProvider {
  protected static final Logger LOG = Logger.getInstance("#com.intellij.ide.errorTreeView.NewErrorTreeViewPanel");
  private volatile String myProgressText = "";
  private volatile float myFraction;
  private final ErrorViewStructure myErrorViewStructure;
  private final ErrorViewTreeBuilder myBuilder;
  private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private volatile boolean myIsDisposed;
  private final ErrorTreeViewConfiguration myConfiguration;


  private ActionToolbar myLeftToolbar;
  protected Project myProject;
  protected Tree myTree;
  private final JPanel myMessagePanel;

  private JLabel myProgressLabel;
  private JPanel myProgressPanel;

  private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;
  private final MyOccurrenceNavigatorSupport myOccurrenceNavigatorSupport;


  public PantsConsoleViewPanel(Project project) {
    myProject = project;
    myConfiguration = ErrorTreeViewConfiguration.getInstance(project);
    setLayout(new BorderLayout());

    myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return myConfiguration.isAutoscrollToSource();
      }

      @Override
      protected void setAutoScrollMode(boolean state) {
        myConfiguration.setAutoscrollToSource(state);
      }
    };

    myMessagePanel = new JPanel(new BorderLayout());

    myErrorViewStructure = createErrorViewStructure(project, canHideWarnings());
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    root.setUserObject(myErrorViewStructure.createDescriptor(myErrorViewStructure.getRootElement(), null));
    final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    myTree = createTree(treeModel);
    myTree.getEmptyText().setText(IdeBundle.message("errortree.noMessages"));
    myBuilder = new ErrorViewTreeBuilder(myTree, treeModel, myErrorViewStructure);

    myOccurrenceNavigatorSupport = new MyOccurrenceNavigatorSupport(myTree);

    myAutoScrollToSourceHandler.install(myTree);
    TreeUtil.installActions(myTree);
    UIUtil.setLineStyleAngled(myTree);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myTree.setLargeModel(true);

    JScrollPane scrollPane = NewErrorTreeRenderer.install(myTree);
    scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
    myMessagePanel.add(PantsConsoleManager.getOrMakeNewConsole(myProject).getComponent(), BorderLayout.CENTER);

    add(createToolbarPanel(null), BorderLayout.WEST);

    add(myMessagePanel, BorderLayout.CENTER);

    myTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          navigateToSource(false);
        }
      }
    });

    EditSourceOnDoubleClickHandler.install(myTree);
  }

  @NotNull
  protected Tree createTree(@NotNull final DefaultTreeModel treeModel) {
    return new Tree(treeModel) {
      @Override
      public void setRowHeight(int i) {
        super.setRowHeight(0);
        // this is needed in order to make UI calculate the height for each particular row
      }
    };
  }

  protected ErrorViewStructure createErrorViewStructure(Project project, boolean canHideWarnings) {
    return new ErrorViewStructure(project, canHideWarnings);
  }

  @Override
  public void dispose() {
    myIsDisposed = true;
    myErrorViewStructure.clear();
    myUpdateAlarm.cancelAllRequests();
    Disposer.dispose(myUpdateAlarm);
    Disposer.dispose(myBuilder);
  }

  @Override
  public void performCopy(@NotNull DataContext dataContext) {
    List<ErrorTreeNodeDescriptor> descriptors = getSelectedNodeDescriptors();
    if (!descriptors.isEmpty()) {
      CopyPasteManager.getInstance().setContents(new StringSelection(StringUtil.join(descriptors, descriptor -> {
        ErrorTreeElement element = descriptor.getElement();
        return NewErrorTreeRenderer.calcPrefix(element) + StringUtil.join(element.getText(), "\n");
      }, "\n")));
    }
  }

  @Override
  public boolean isCopyEnabled(@NotNull DataContext dataContext) {
    return !getSelectedNodeDescriptors().isEmpty();
  }

  @Override
  public boolean isCopyVisible(@NotNull DataContext dataContext) {
    return true;
  }

  @Override
  public void addMessage(int type, @NotNull String[] text, @Nullable VirtualFile file, int line, int column, @Nullable Object data) {
    addMessage(type, text, null, file, line, column, data);
  }

  @Override
  public void addMessage(
    int type,
    @NotNull String[] text,
    @Nullable VirtualFile underFileGroup,
    @Nullable VirtualFile file,
    int line,
    int column,
    @Nullable Object data
  ) {
    if (myIsDisposed) {
      return;
    }
    myErrorViewStructure
      .addMessage(ErrorTreeElementKind.convertMessageFromCompilerErrorType(type), text, underFileGroup, file, line, column, data);
    myBuilder.updateTree();
  }

  @Override
  public void addMessage(
    int type,
    @NotNull String[] text,
    @Nullable String groupName,
    @NotNull Navigatable navigatable,
    @Nullable String exportTextPrefix,
    @Nullable String rendererTextPrefix,
    @Nullable Object data
  ) {
    if (myIsDisposed) {
      return;
    }
    VirtualFile file = data instanceof VirtualFile ? (VirtualFile) data : null;
    if (file == null && navigatable instanceof OpenFileDescriptor) {
      file = ((OpenFileDescriptor) navigatable).getFile();
    }
    final String exportPrefix = exportTextPrefix == null ? "" : exportTextPrefix;
    final String renderPrefix = rendererTextPrefix == null ? "" : rendererTextPrefix;
    final ErrorTreeElementKind kind = ErrorTreeElementKind.convertMessageFromCompilerErrorType(type);
    myErrorViewStructure.addNavigatableMessage(groupName, navigatable, kind, text, data, exportPrefix, renderPrefix, file);
    myBuilder.updateTree();
  }

  public ErrorViewStructure getErrorViewStructure() {
    return myErrorViewStructure;
  }

  public static String createExportPrefix(int line) {
    return line < 0 ? "" : IdeBundle.message("errortree.prefix.line", line);
  }

  public static String createRendererPrefix(int line, int column) {
    if (line < 0) return "";
    if (column < 0) return "(" + line + ")";
    return "(" + line + ", " + column + ")";
  }

  @Override
  @NotNull
  public JComponent getComponent() {
    return this;
  }

  @Nullable
  private NavigatableMessageElement getSelectedMessageElement() {
    final ErrorTreeElement selectedElement = getSelectedErrorTreeElement();
    return selectedElement instanceof NavigatableMessageElement ? (NavigatableMessageElement) selectedElement : null;
  }

  @Nullable
  public ErrorTreeElement getSelectedErrorTreeElement() {
    final ErrorTreeNodeDescriptor treeNodeDescriptor = getSelectedNodeDescriptor();
    return treeNodeDescriptor == null ? null : treeNodeDescriptor.getElement();
  }

  @Nullable
  public ErrorTreeNodeDescriptor getSelectedNodeDescriptor() {
    List<ErrorTreeNodeDescriptor> descriptors = getSelectedNodeDescriptors();
    return descriptors.size() == 1 ? descriptors.get(0) : null;
  }

  private List<ErrorTreeNodeDescriptor> getSelectedNodeDescriptors() {
    TreePath[] paths = myTree.getSelectionPaths();
    if (paths == null) {
      return Collections.emptyList();
    }
    List<ErrorTreeNodeDescriptor> result = ContainerUtil.newArrayList();
    for (TreePath path : paths) {
      DefaultMutableTreeNode lastPathNode = (DefaultMutableTreeNode) path.getLastPathComponent();
      Object userObject = lastPathNode.getUserObject();
      if (userObject instanceof ErrorTreeNodeDescriptor) {
        result.add((ErrorTreeNodeDescriptor) userObject);
      }
    }
    return result;
  }

  private void navigateToSource(final boolean focusEditor) {
    NavigatableMessageElement element = getSelectedMessageElement();
    if (element == null) {
      return;
    }
    final Navigatable navigatable = element.getNavigatable();
    if (navigatable.canNavigate()) {
      navigatable.navigate(focusEditor);
    }
  }

  public void close() {
    MessageView messageView = MessageView.SERVICE.getInstance(myProject);
    Content content = messageView.getContentManager().getContent(this);
    if (content != null) {
      messageView.getContentManager().removeContent(content, true);
    }
  }

  private void updateProgress() {
    if (myIsDisposed) {
      return;
    }
    myUpdateAlarm.cancelAllRequests();
    myUpdateAlarm.addRequest(() -> {
      final float fraction = myFraction;
      final String text = myProgressText;
      if (fraction > 0.0f) {
        myProgressLabel.setText((int) (fraction * 100 + 0.5) + "%  " + text);
      }
      else {
        myProgressLabel.setText(text);
      }
    }, 50, ModalityState.NON_MODAL);
  }


  private void initProgressPanel() {
    if (myProgressPanel == null) {
      myProgressPanel = new JPanel(new GridLayout(1, 2));
      myProgressLabel = new JLabel();
      myProgressPanel.add(myProgressLabel);
      //JLabel secondLabel = new JLabel();
      //myProgressPanel.add(secondLabel);
      myMessagePanel.add(myProgressPanel, BorderLayout.SOUTH);
      myMessagePanel.validate();
    }
  }

  public void collapseAll() {
    TreeUtil.collapseAll(myTree, 2);
  }


  public void expandAll() {
    TreePath[] selectionPaths = myTree.getSelectionPaths();
    TreePath leadSelectionPath = myTree.getLeadSelectionPath();
    int row = 0;
    while (row < myTree.getRowCount()) {
      myTree.expandRow(row);
      row++;
    }

    if (selectionPaths != null) {
      // restore selection
      myTree.setSelectionPaths(selectionPaths);
    }
    if (leadSelectionPath != null) {
      // scroll to lead selection path
      myTree.scrollPathToVisible(leadSelectionPath);
    }
  }

  private JPanel createToolbarPanel(@Nullable Runnable rerunAction) {
    AnAction closeMessageViewAction = new CloseTabToolbarAction() {
      @Override
      public void actionPerformed(AnActionEvent e) {
        close();
        PantsConsoleManager.getOrMakeNewConsole(myProject).print("hello", ConsoleViewContentType.NORMAL_OUTPUT);
      }
    };

    DefaultActionGroup leftUpdateableActionGroup = new DefaultActionGroup();
    leftUpdateableActionGroup.add(closeMessageViewAction);

    JPanel toolbarPanel = new JPanel(new BorderLayout());
    ActionManager actionManager = ActionManager.getInstance();
    myLeftToolbar = actionManager.createActionToolbar(ActionPlaces.COMPILER_MESSAGES_TOOLBAR, leftUpdateableActionGroup, false);
    toolbarPanel.add(myLeftToolbar.getComponent(), BorderLayout.WEST);

    return toolbarPanel;
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return myOccurrenceNavigatorSupport.goNextOccurence();
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return myOccurrenceNavigatorSupport.goPreviousOccurence();
  }

  @Override
  public boolean hasNextOccurence() {
    return myOccurrenceNavigatorSupport.hasNextOccurence();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return myOccurrenceNavigatorSupport.hasPreviousOccurence();
  }

  @Override
  public String getNextOccurenceActionName() {
    return myOccurrenceNavigatorSupport.getNextOccurenceActionName();
  }

  @Override
  public String getPreviousOccurenceActionName() {
    return myOccurrenceNavigatorSupport.getPreviousOccurenceActionName();
  }

  protected boolean canHideWarnings() {
    return true;
  }


  private static class MyOccurrenceNavigatorSupport extends OccurenceNavigatorSupport {
    public MyOccurrenceNavigatorSupport(final Tree tree) {
      super(tree);
    }

    @Override
    protected Navigatable createDescriptorForNode(DefaultMutableTreeNode node) {
      Object userObject = node.getUserObject();
      if (!(userObject instanceof ErrorTreeNodeDescriptor)) {
        return null;
      }
      final ErrorTreeNodeDescriptor descriptor = (ErrorTreeNodeDescriptor) userObject;
      final ErrorTreeElement element = descriptor.getElement();
      if (element instanceof NavigatableMessageElement) {
        return ((NavigatableMessageElement) element).getNavigatable();
      }
      return null;
    }

    @Override
    public String getNextOccurenceActionName() {
      return IdeBundle.message("action.next.message");
    }

    @Override
    public String getPreviousOccurenceActionName() {
      return IdeBundle.message("action.previous.message");
    }
  }

  @Override
  public List<Object> getGroupChildrenData(final String groupName) {
    return myErrorViewStructure.getGroupChildrenData(groupName);
  }

  @Override
  public void removeGroup(final String name) {
    myErrorViewStructure.removeGroup(name);
  }

  @Override
  public void addFixedHotfixGroup(String text, List<SimpleErrorData> children) {
    myErrorViewStructure.addFixedHotfixGroup(text, children);
  }

  @Override
  public void addHotfixGroup(HotfixData hotfixData, List<SimpleErrorData> children) {
    myErrorViewStructure.addHotfixGroup(hotfixData, children, this);
  }

  @Override
  public void reload() {
    myBuilder.updateTree();
  }
}
