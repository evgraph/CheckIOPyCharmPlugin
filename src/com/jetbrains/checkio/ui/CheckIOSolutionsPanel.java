package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOPublicationCategory;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;

public class CheckIOSolutionsPanel extends JPanel {
  private Project myProject;
  private HashMap<CheckIOPublicationCategory, ArrayList<CheckIOPublication>> myCategoryArrayListHashMap;
  private ArrayList<CheckIOPublication> clearPublications;
  private ArrayList<CheckIOPublication> speedyPublications;
  private ArrayList<CheckIOPublication> creativePublications;
  private JPanel publicationInfoPanel;
  private Tree tree;
  private Task task;
  private static final Logger LOG = Logger.getInstance(CheckIOSolutionsPanel.class);

  private JButton backButton;

  public CheckIOSolutionsPanel(@NotNull final CheckIOPublication[] publications, @NotNull final Project project,
                               @NotNull final CheckIOToolWindow toolWindow) {
    myProject = project;
    setLayout(new BorderLayout());
    clearPublications = new ArrayList<>();
    speedyPublications = new ArrayList<>();
    creativePublications = new ArrayList<>();

    initHashMap();
    setPublicationsByCategory(publications);
    final JPanel solutionsPanel = createSolutionsPanel();
    publicationInfoPanel = new JPanel();
    add(publicationInfoPanel, BorderLayout.PAGE_START);
    add(Box.createVerticalStrut(10));
    add(solutionsPanel, BorderLayout.WEST);
    add(createButtonPanel(toolWindow), BorderLayout.PAGE_END);
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
    solutionsPanel.add(tree, BorderLayout.WEST);
    return solutionsPanel;
  }

  private Tree createSolutionsTree() {
    task = CheckIOUtils.getTaskFromSelectedEditor(myProject);
    if (task == null) {
      LOG.warn("Request solutions for null task");
      return new Tree();
    }
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(task.getName());
    final Tree tree = new Tree(root);
    tree.setPreferredSize(new Dimension(400, 800));
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
    return tree;
  }


  private JPanel createButtonPanel(@NotNull final CheckIOToolWindow toolWindow) {
    JPanel buttonPanel = new JPanel();
    backButton = new JButton(AllIcons.Actions.Back);
    backButton.setAlignmentX(Component.LEFT_ALIGNMENT);

    backButton.addActionListener(e -> toolWindow.showTaskInfoPanel());
    buttonPanel.add(backButton);
    return buttonPanel;
  }


  private void setPublicationsInfoPanel(@NotNull final CheckIOPublication publication) {
    final BoxLayout layout = new BoxLayout(publicationInfoPanel, BoxLayout.PAGE_AXIS);
    publicationInfoPanel.setLayout(layout);
    publicationInfoPanel.removeAll();
    final JLabel userNameLabel =
      new JLabel(UIUtil.toHtml("<b>User: </b>" + "<a href=\"\">" + publication.myAuthor.getUsername() + "</a>", 5));
    final JLabel userLevelLabel = new JLabel(UIUtil.toHtml("<b>Level: </b>" + publication.myAuthor.getLevel(), 5));
    final JLabel viewOnWebLabel = new JLabel(UIUtil.toHtml(publication.myCategory + " <a href=\"\">solution</a> for " + task.getName()));
    userNameLabel.addMouseListener(new MyMouseListener(CheckIOUtils.getUserProfileLink(publication.myAuthor)));
    viewOnWebLabel.addMouseListener(new MyMouseListener(CheckIOUtils.getPublicationLink(publication)));


    publicationInfoPanel.add(viewOnWebLabel);
    publicationInfoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    publicationInfoPanel.add(userNameLabel);
    publicationInfoPanel.add(userLevelLabel);
    publicationInfoPanel.add(Box.createRigidArea(new Dimension(0, 20)));
  }

  private static class MyMouseListener implements MouseListener {
    private String url;

    public MyMouseListener(@NotNull final String url) {
      this.url = url;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      BrowserUtil.browse(url);
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
  }

  private class MyTreeSelectionListener implements TreeSelectionListener {
    @Override
    public void valueChanged(TreeSelectionEvent e) {
      final TreePath treePath = e.getPath();
      final String publicationName = treePath.getLastPathComponent().toString();
      final String publicationFileName = publicationName + ".py";
      final VirtualFile publicationFile = CheckIOUtils.getPublicationFile(myProject, publicationFileName, task);
      if (publicationFile != null) {
        DefaultMutableTreeNode[] nodes = tree.getSelectedNodes(DefaultMutableTreeNode.class, null);
        for (DefaultMutableTreeNode node : nodes) {
          CheckIOPublication publication = (CheckIOPublication)node.getUserObject();
          if (publication.getPublicationName().equals(publicationName)) {
            setPublicationsInfoPanel(publication);
          }
        }
        FileEditorManager.getInstance(myProject).openFile(publicationFile, true);
        ProjectView.getInstance(myProject).refresh();
      }
    }
  }
}