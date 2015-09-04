package com.jetbrains.checkio.ui;

import com.intellij.ide.BrowserUtil;
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
import com.jetbrains.checkio.CheckIOConnector;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.edu.courseFormat.Task;
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

public class CheckIOPublicationsPanel extends JPanel {
  private PublicationsPanel publicationInfoPanel;
  private JScrollPane mySolutionsPanel;
  private Project myProject;
  private Tree tree;
  private Task task;
  private HashMap<String, CheckIOPublication[]> myCategoryArrayListHashMap;
  private static final Logger LOG = Logger.getInstance(CheckIOPublicationsPanel.class);

  public CheckIOPublicationsPanel(@NotNull final Project project) {
    myProject = project;
  }

  public void update(@NotNull final HashMap<String, CheckIOPublication[]> publicationByCategory,
                     @NotNull final JPanel buttonPanel) {
    this.removeAll();
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    myCategoryArrayListHashMap = publicationByCategory;
    final JPanel contentPanel = createContentPanel();
    openFirstSolution();
    add(buttonPanel);
    add(contentPanel);
  }

  private JPanel createContentPanel() {
    final JPanel contentPanel = new JPanel(new BorderLayout());
    publicationInfoPanel = new PublicationsPanel();
    mySolutionsPanel = createSolutionsPanel();
    updateLafIfNeeded(contentPanel);
    contentPanel.add(publicationInfoPanel, BorderLayout.PAGE_START);
    contentPanel.add(mySolutionsPanel, BorderLayout.WEST);
    contentPanel.add(createSeeMoreSolutionsLabel(), BorderLayout.PAGE_END);
    return contentPanel;
  }

  private void updateLafIfNeeded(@NotNull final JPanel contentPanel) {
    if (!(LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo)) {
      publicationInfoPanel.setBackground(UIUtil.getTreeBackground());
      mySolutionsPanel.setBackground(UIUtil.getTreeBackground());
      contentPanel.setBackground(UIUtil.getTreeBackground());
    }
  }

  private JScrollPane createSolutionsPanel() {
    tree = createSolutionsTree();
    TreeUtil.expandAll(tree);

    return new JBScrollPane(tree);
  }

  private HyperlinkLabel createSeeMoreSolutionsLabel() {
    final HyperlinkLabel hyperlinkLabel = new HyperlinkLabel();
    hyperlinkLabel.setHyperlinkText("See more solutions on web");
    hyperlinkLabel.setHyperlinkTarget(CheckIOConnector.getSeePublicationsOnWebLink(task.getName()));
    return hyperlinkLabel;
  }


  private void openFirstSolution() {
    TreePath parent = new TreePath(tree.getModel().getRoot());
    final TreeNode node = (TreeNode)parent.getLastPathComponent();
    final TreeNode categoryNode = node.getChildAt(0);
    final TreeNode child = categoryNode.getChildAt(0);
    tree.addSelectionPath(TreeUtil.getPathFromRoot(child));
  }

  private Tree createSolutionsTree() {
    task = CheckIOUtils.getTaskFromSelectedEditor(myProject);
    if (task == null) {
      LOG.warn("Request solutions for null task");
      return new Tree();
    }
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(task.getName());
    final Tree tree = new Tree(root);
    tree.setRootVisible(false);
    tree.setPreferredSize(new Dimension(CheckIOUtils.MAX_WIDTH, CheckIOUtils.HEIGHT));
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

  private class PublicationsPanel extends JPanel {
    private final JLabel myViewOnWebLabel;
    private final JLabel myUserNameLabel;
    private final JLabel myUserLevelLabel;
    private CheckIOPublication myPublication;

    public PublicationsPanel() {
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
      myUserNameLabel.setToolTipText("Click to see user profile on web");
      myViewOnWebLabel.setToolTipText("Click to see solution on web");
    }

    public void setUserInfo(@NotNull final CheckIOPublication publication) {
      myPublication = publication;
      myUserNameLabel.setText(UIUtil.toHtml("<b>User: </b>" + "<a href=\"\">" + myPublication.getAuthor().getUsername() + "</a>", 5));
      myUserLevelLabel.setText(UIUtil.toHtml("<b>Level: </b>" + myPublication.getAuthor().getLevel(), 5));
      myViewOnWebLabel.setText(UIUtil.toHtml(myPublication.getCategory() + " <a href=\"\">solution</a> for " + task.getName()));
    }


    private class MyMouseListener extends MouseAdapter {
      private ListenerKind kind;

      public MyMouseListener(ListenerKind kind) {
        this.kind = kind;
      }

      private String getUrl() {
        String url = "";
        if (myPublication != null) {
          if (kind == ListenerKind.Publication) {
            final String token = CheckIOTaskManager.getInstance(myProject).accessToken;
            url = myPublication.getPublicationLink(token, task.getName());
          }
          else {
            url = CheckIOUtils.getUserProfileLink(myPublication.getAuthor());
          }
        }
        return url;
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        final String url = getUrl();
        BrowserUtil.browse(url);
      }
    }
  }

  enum ListenerKind {
    Publication, User
  }


  private class MyTreeSelectionListener implements TreeSelectionListener {
    @Override
    public void valueChanged(TreeSelectionEvent e) {
      final com.intellij.openapi.progress.Task.Backgroundable task = getLoadingSolutionTask(e);
      ProgressManager.getInstance().run(task);
    }

    @NotNull
    private com.intellij.openapi.progress.Task.Backgroundable getLoadingSolutionTask(final TreeSelectionEvent e) {
      return new com.intellij.openapi.progress.Task.Backgroundable(myProject, "Loading solution", false) {

        @Override
        public void onCancel() {
          Thread.currentThread().interrupt();
        }

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


          final String token = CheckIOTaskManager.getInstance(myProject).accessToken;
          try {
            CheckIOConnector.setPublicationCodeAndCategoryFromRequest(token, publication);
            final File
              publicationFile = CheckIOUtils.createPublicationFile(myProject, CheckIOPublicationsPanel.this.task.getName(), publication);
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
        final Icon icon = myIconsForRunners.get(publication.getInterpreter());
        setIcon(icon);
      }
      else {
        setIcon(CheckIOIcons.SHOW_SOLUTIONS);
      }
      return this;
    }
  }
}