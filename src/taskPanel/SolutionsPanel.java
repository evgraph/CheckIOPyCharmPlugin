package taskPanel;

import com.intellij.icons.AllIcons;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class SolutionsPanel extends JPanel {
  private static final String CLEAR_SOLUTIONS = "Clear solutions";
  private static final String CREATIVE_SOLUTIONS = "Creative solutions";
  private static final String SPEEDY_SOLUTIONS = "Speedy solutions";
  private JButton toTaskDescription;

  public SolutionsPanel() {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    JPanel clearSolutionsPanel = createSolutionsPanel(CLEAR_SOLUTIONS);
    JPanel creativeSolutionsPanel = createSolutionsPanel(CREATIVE_SOLUTIONS);
    JPanel speedySolutionsPanel = createSolutionsPanel(SPEEDY_SOLUTIONS);
    add(clearSolutionsPanel);
    add(creativeSolutionsPanel);
    add(speedySolutionsPanel);
    add(createButtonPanel());
    add(createButtonPanel());
  }

  private static JPanel createSolutionsPanel(String panelName) {
    JPanel solutionsPanel = new JPanel();
    solutionsPanel.add(createSolutionsTree(panelName));
    return solutionsPanel;
  }

  private static Tree createSolutionsTree(String rootName) {
    final DefaultMutableTreeNode top = new DefaultMutableTreeNode(rootName);

    for (int i = 0; i < 5; i++) {
      top.add(new DefaultMutableTreeNode("Solutions " + (i + 1)));
    }
    Tree tree = new Tree(top);
    tree.setPreferredSize(new Dimension(400, 200));
    return tree;
  }

  public JButton getToTaskDescription() {
    return toTaskDescription;
  }

  public void setToTaskDescription(JButton toTaskDescription) {
    this.toTaskDescription = toTaskDescription;
  }

  private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel();
    setToTaskDescription(new JButton(AllIcons.Actions.Back));
    toTaskDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonPanel.add(toTaskDescription);
    return buttonPanel;
  }
}