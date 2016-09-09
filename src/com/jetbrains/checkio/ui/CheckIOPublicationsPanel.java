package com.jetbrains.checkio.ui;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TreeUIHelper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.connectors.CheckIOPublicationGetter;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.edu.learning.courseFormat.Task;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CheckIOPublicationsPanel extends JPanel {
  private static final Logger LOG = Logger.getInstance(CheckIOPublicationsPanel.class);
  private final Project myProject;
  private Map<String, CheckIOPublication[]> myCategoryArrayListHashMap;
  private PublicationInfoPanel publicationInfoPanel;
  private JScrollPane mySolutionTreePanel;
  private JPanel myButtonPanel;
  private Tree tree;
  private Task task;

  public CheckIOPublicationsPanel(@NotNull final Project project) {
    myProject = project;
  }

  public void update(@NotNull final Map<String, CheckIOPublication[]> publicationsByCategory,
                     @NotNull final JPanel buttonPanel) throws IllegalStateException {
    this.removeAll();
    setLayout(new BorderLayout());

    myCategoryArrayListHashMap = publicationsByCategory;
    myButtonPanel = buttonPanel;
    publicationInfoPanel = new PublicationInfoPanel();
    mySolutionTreePanel = createSolutionTreePanel();

    add(combineButtonAndPublicationInfoPanels(), BorderLayout.PAGE_START);
    add(mySolutionTreePanel, BorderLayout.CENTER);
    add(createSeeMoreSolutionsLabel(), BorderLayout.PAGE_END);

    openFirstSolution();
    updateLafIfNeeded(mySolutionTreePanel);
  }


  private JPanel combineButtonAndPublicationInfoPanels() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(myButtonPanel, BorderLayout.PAGE_START);
    panel.add(publicationInfoPanel, BorderLayout.CENTER);
    return panel;
  }

  private JScrollPane createSolutionTreePanel() throws IllegalStateException {
    tree = createSolutionsTree();
    TreeUtil.expandAll(tree);

    final JBScrollPane pane = new JBScrollPane(tree);
    pane.setBorder(null);
    return pane;
  }

  private Tree createSolutionsTree() throws IllegalStateException {
    task = CheckIOUtils.getTaskFromSelectedEditor(myProject);
    if (task == null) {
      throw new IllegalStateException("Request solutions for null task");
    }
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(task.getName());
    final Tree tree = new Tree(root);
    tree.setRootVisible(false);
    tree.addTreeSelectionListener(new MyTreeSelectionListener());

    for (String category : myCategoryArrayListHashMap.keySet()) {
      final DefaultMutableTreeNode top = new DefaultMutableTreeNode(category + " solutions");
      root.add(top);
      final CheckIOPublication[] publications = myCategoryArrayListHashMap.get(category);

      for (CheckIOPublication publication : publications) {
        DefaultMutableTreeNode treeNode = (new DefaultMutableTreeNode(publication, false));
        top.add(treeNode);
      }
    }
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setCellRenderer(new MyRenderer());
    TreeUIHelper.getInstance().installTreeSpeedSearch(tree);

    return tree;
  }

  private HyperlinkLabel createSeeMoreSolutionsLabel() {
    final HyperlinkLabel hyperlinkLabel = new HyperlinkLabel();
    hyperlinkLabel.setHyperlinkText(CheckIOBundle.message("publication.see.more.on.web"));
    hyperlinkLabel.setHyperlinkTarget(CheckIOUtils.getSeePublicationsOnWebLink(task.getName()));
    return hyperlinkLabel;
  }


  private void openFirstSolution() {
    TreePath parent = new TreePath(tree.getModel().getRoot());
    final TreeNode node = (TreeNode)parent.getLastPathComponent();
    final TreeNode categoryNode = node.getChildAt(0);
    final TreeNode child = categoryNode.getChildAt(0);
    tree.addSelectionPath(TreeUtil.getPathFromRoot(child));
  }

  private void updateLafIfNeeded(@NotNull final JScrollPane contentPanel) {
    if (!(LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo)) {
      publicationInfoPanel.setBackground(UIUtil.getTreeBackground());
      mySolutionTreePanel.setBackground(UIUtil.getTreeBackground());
      contentPanel.setBackground(UIUtil.getTreeBackground());
    }
  }


  enum ListenerKind {
    Publication, User
  }

  private static class MyRenderer extends DefaultTreeCellRenderer {
    private final HashMap<String, Icon> myIconsForRunners = new HashMap<String, Icon>() {
      {
        put("python-27", CheckIOIcons.PYTHON_2);
        put("python-3", CheckIOIcons.PYTHON_3);
      }
    };

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      super.getTreeCellRendererComponent(
        tree, value, selected,
        expanded, leaf, row,
        hasFocus);

      if (leaf) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        final CheckIOPublication publication = (CheckIOPublication)node.getUserObject();
        if (publication != null) {
          final Icon icon = myIconsForRunners.get(publication.getInterpreter());
          setIcon(icon);
        }
      }
      else {
        setIcon(CheckIOIcons.SHOW_SOLUTIONS);
      }
      return this;
    }
  }

  private class PublicationInfoPanel extends JPanel {
    private final JLabel myViewOnWebLabel;
    private final JLabel myUserNameLabel;
    private final JLabel myUserLevelLabel;
    private CheckIOPublication myPublication;

    public PublicationInfoPanel() {
      final BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
      setBorder(BorderFactory.createEtchedBorder());
      setLayout(layout);
      myViewOnWebLabel = new JLabel("");
      myUserNameLabel = new JLabel();
      myUserLevelLabel = new JLabel();
      add(Box.createRigidArea(new Dimension(0, 10)));
      add(myViewOnWebLabel);
      add(Box.createRigidArea(new Dimension(0, 10)));
      add(myUserNameLabel);
      add(myUserLevelLabel);
      add(Box.createRigidArea(new Dimension(0, 20)));
      myUserNameLabel.addMouseListener(new MyMouseListener(ListenerKind.User));
      myViewOnWebLabel.addMouseListener(new MyMouseListener(ListenerKind.Publication));
      myUserNameLabel.setToolTipText(CheckIOBundle.message("publication.action.user.profile"));
      myViewOnWebLabel.setToolTipText(CheckIOBundle.message("publication.action.solution.on.web"));
    }

    public void setUserInfo(@NotNull final CheckIOPublication publication) {
      myPublication = publication;
      myUserNameLabel.setText(UIUtil.toHtml("<b>User: </b>" + "<a href=\"\">" + myPublication.getAuthor().getUsername() + "</a>", 5));
      myUserLevelLabel.setText(UIUtil.toHtml("<b>Level: </b>" + myPublication.getAuthor().getLevel(), 5));
      myViewOnWebLabel.setText(UIUtil.toHtml(
        StringUtils.capitalize(myPublication.getCategory()) + " <a href=\"\">solution</a> for " + task.getName()));
    }

    private class MyMouseListener extends MouseAdapter {
      private final ListenerKind kind;

      public MyMouseListener(ListenerKind kind) {
        this.kind = kind;
      }

      private String getUrl() {
        String url = "";
        try {
          if (myPublication != null) {
            if (kind == ListenerKind.Publication) {
              final String token = CheckIOTaskManager.getInstance(myProject).getAccessTokenAndUpdateIfNeeded(myProject);
              url = myPublication.getPublicationLink(token, task.getName());
            }
            else {
              url = myPublication.getAuthor().getUserProfileLink();
            }
          }
        }
        catch (IOException e) {
          CheckIOUtils.makeNoInternetConnectionNotifier(myProject);
        }
        return url;
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        final String url = getUrl();
        final CheckIOBrowserWindow window = new CheckIOBrowserWindow();
        window.setShowProgress(true);
        window.load(url);
        window.setVisible(true);
      }
    }
  }

  private class MyTreeSelectionListener implements TreeSelectionListener {
    @Override
    public void valueChanged(TreeSelectionEvent e) {
      if (e.isAddedPath()) {
        final com.intellij.openapi.progress.Task.Backgroundable task = getLoadingSolutionTask(e);
        ProgressManager.getInstance().run(task);
      }
    }

    @NotNull
    private com.intellij.openapi.progress.Task.Backgroundable getLoadingSolutionTask(final TreeSelectionEvent e) {
      return new com.intellij.openapi.progress.Task.Backgroundable(myProject, CheckIOBundle.message("publication.loading.process.message"),
                                                                   false) {

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          final TreePath treePath = e.getPath();
          final TreeNode selectedNode = (TreeNode)treePath.getLastPathComponent();
          if (!selectedNode.isLeaf()) {
            return;
          }
          DefaultMutableTreeNode[] nodes = tree.getSelectedNodes(DefaultMutableTreeNode.class, null);
          DefaultMutableTreeNode node = nodes[0];

          final CheckIOPublication publication = (CheckIOPublication)node.getUserObject();

          try {
            final String token = CheckIOTaskManager.getInstance(myProject).getAccessTokenAndUpdateIfNeeded(myProject);
            final Future<?> future =
              ApplicationManager.getApplication().executeOnPooledThread(() -> getPublicationInfoAndOpenFile(publication, token));

            while (!future.isDone()) {
              indicator.checkCanceled();
              try {
                TimeUnit.MILLISECONDS.sleep(500);
              }
              catch (InterruptedException e) {
                LOG.info(e.getMessage());
              }
            }
          }
          catch (IOException e1) {
            CheckIOUtils.makeNoInternetConnectionNotifier(myProject);
          }
        }

        private void getPublicationInfoAndOpenFile(CheckIOPublication publication, String token) {
          try {
            CheckIOPublicationGetter.setPublicationCodeAndCategoryFromRequest(token, publication);
            final File
              publicationFile = CheckIOUtils
              .createPublicationFile(myProject, CheckIOPublicationsPanel.this.task.getName(), publication);
            final VirtualFile virtualPublicationFile = VfsUtil.findFileByIoFile(publicationFile, true);
            if (virtualPublicationFile != null) {
              ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(
                () -> {
                  publicationInfoPanel.setUserInfo(publication);
                  virtualPublicationFile.putUserDataIfAbsent(CheckIOUtils.CHECKIO_LANGUAGE_LEVEL_KEY, publication.getLanguageLevel());
                  FileEditorManager.getInstance(myProject).openFile(virtualPublicationFile, true);
                }), ModalityState.defaultModalityState());
            }
          }
          catch (IOException e1) {
            LOG.warn(e1.getMessage());
            CheckIOUtils.makeNoInternetConnectionNotifier(myProject);
          }
        }
      };
    }
  }
}