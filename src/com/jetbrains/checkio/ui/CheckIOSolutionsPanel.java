package com.jetbrains.checkio.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TreeUIHelper;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOPublicationCategory;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

public class CheckIOSolutionsPanel extends JPanel {
  private Project myProject;
  private HashMap<CheckIOPublicationCategory, ArrayList<CheckIOPublication>> myCategoryArrayListHashMap;
  private ArrayList<CheckIOPublication> clearPublications;
  private ArrayList<CheckIOPublication> speedyPublications;
  private ArrayList<CheckIOPublication> creativePublications;
  private PublicationsPanel publicationInfoPanel;
  private Tree tree;
  private Task task;
  private static final Logger LOG = Logger.getInstance(CheckIOSolutionsPanel.class);
  private JPanel contentPanel;

  public CheckIOSolutionsPanel(@NotNull final CheckIOPublication[] publications, @NotNull final Project project,
                               @NotNull final CheckIOToolWindow toolWindow) {
    myProject = project;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    clearPublications = new ArrayList<>();
    speedyPublications = new ArrayList<>();
    creativePublications = new ArrayList<>();

    initHashMap();
    setPublicationsByCategory(publications);
    setContentPanel();
    add(CheckIOToolWindow.createButtonPanel(toolWindow));
    add(contentPanel);
  }

  private void setContentPanel() {
    contentPanel = new JPanel(new BorderLayout());
    final JPanel solutionsPanel = createSolutionsPanel();
    publicationInfoPanel = new PublicationsPanel();

    contentPanel.add(publicationInfoPanel, BorderLayout.PAGE_START);
    contentPanel.add(solutionsPanel, BorderLayout.WEST);
  }

  private void initHashMap() {
    myCategoryArrayListHashMap = new HashMap<CheckIOPublicationCategory, ArrayList<CheckIOPublication>>() {
      {
        put(CheckIOPublicationCategory.Clear, clearPublications);
        put(CheckIOPublicationCategory.Creative, creativePublications);
        put(CheckIOPublicationCategory.Speedy, speedyPublications);
      }
    };
  }

  private void setPublicationsByCategory(@NotNull final CheckIOPublication[] publications) {
    for (CheckIOPublication publication : publications) {
      myCategoryArrayListHashMap.get(publication.myCategory).add(publication);
    }
  }

  private JPanel createSolutionsPanel() {
    JPanel solutionsPanel = new JPanel(new BorderLayout());
    tree = createSolutionsTree();
    TreeUtil.expandAll(tree);
    openFirstSolution();

    solutionsPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.PAGE_START);
    solutionsPanel.add(tree, BorderLayout.WEST);
    return solutionsPanel;
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
    tree.setPreferredSize(new Dimension(CheckIOUtils.height, CheckIOUtils.height));
    tree.addTreeSelectionListener(new MyTreeSelectionListener());

    for (CheckIOPublicationCategory category : CheckIOPublicationCategory.values()) {
      final DefaultMutableTreeNode top = new DefaultMutableTreeNode(category.toString() + " solutions");
      root.add(top);
      final ArrayList<CheckIOPublication> publications = myCategoryArrayListHashMap.get(category);

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
    private JLabel myViewOnWebLabel;
    private JLabel myUserNameLabel;
    private JLabel myUserLevelLabel;
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
    }

    public void setUserInfo(@NotNull final CheckIOPublication publication) {
      myPublication = publication;
      myUserNameLabel.setText(UIUtil.toHtml("<b>User: </b>" + "<a href=\"\">" + myPublication.myAuthor.getUsername() + "</a>", 5));
      myUserLevelLabel.setText(UIUtil.toHtml("<b>Level: </b>" + myPublication.myAuthor.getLevel(), 5));
      myViewOnWebLabel.setText(UIUtil.toHtml(myPublication.myCategory + " <a href=\"\">solution</a> for " + task.getName()));
    }


    private class MyMouseListener extends MouseAdapter {
      private String url = "";

      public MyMouseListener(ListenerKind kind) {
        if (myPublication != null) {
          if (kind == ListenerKind.Publication) {
            url = CheckIOUtils.getPublicationLink(myPublication);
          }
          else {
            url = CheckIOUtils.getUserProfileLink(myPublication.myAuthor);
          }
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
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
      final TreePath treePath = e.getPath();
      final TreeNode selectedNode = (TreeNode)treePath.getLastPathComponent();
      if (!selectedNode.isLeaf()) {
        return;
      }
      final String publicationName = treePath.getLastPathComponent().toString();
      final String publicationFileName = publicationName + ".py";
      final VirtualFile publicationFile = CheckIOUtils.getPublicationFile(myProject, publicationFileName, task);
      if (publicationFile != null) {
        DefaultMutableTreeNode[] nodes = tree.getSelectedNodes(DefaultMutableTreeNode.class, null);
        DefaultMutableTreeNode node = nodes[0];
        CheckIOPublication publication = (CheckIOPublication)node.getUserObject();
        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(
          () -> publicationInfoPanel.setUserInfo(publication)));
        FileEditorManager.getInstance(myProject).openFile(publicationFile, true);
      }
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
        final Icon icon = myIconsForRunners.get(publication.runner);
        setIcon(icon);
      }
      else {
        setIcon(CheckIOIcons.SHOW_SOLUTIONS);
      }
      return this;
    }
  }
}