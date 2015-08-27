package com.jetbrains.checkio.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class CheckIOHintPanel extends JPanel {
  public static final String ID = "Hints";
  private static final Logger LOG = Logger.getInstance(CheckIOHintPanel.class);
  private final LinkedBlockingQueue<JLabel> hintQueue = new LinkedBlockingQueue<>();
  private Task myTask;
  private Project myProject;
  private ScrollablePanel myHintPanel;

  public CheckIOHintPanel(@NotNull Project project) {
    myTask = CheckIOUtils.getTaskFromSelectedEditor(project);
    myProject = project;

    if (myTask == null) {
      LOG.warn("User request hints for an empty editor");
      ToolWindowManager.getInstance(project).unregisterToolWindow(ID);
      return;
    }
    //TODO: change (api needed)
    //final ArrayList<String> hints = taskManager.myTaskHints.get(myTask.getName());

    createHintPanel();
    showNewHint();
  }

  public void createHintPanel() {
    final List<String> hints = Arrays.asList("Try to run the code and see which test fails", "The problem is likely an empty dictionary.",
                                             "Try to add special case processing.",
                                             "Look carefully when your code checks keys and values in the \"for ... in ...\" loop.",
                                             "You can replace a value if it equals {}",

                                             "Try to run the code and see which test fails", "The problem is likely an empty dictionary.",
                                             "Try to add special case processing.",
                                             "Look carefully when your code checks keys and values in the \"for ... in ...\" loop.",
                                             "You can replace a value if it equals {}");

    setLayout(new GridBagLayout());
    final JScrollPane hintsPanel = getHintsPanel(hints);
    setMinimumSize(new Dimension(0, 0));
    setPreferredSize(hintsPanel.getPreferredSize());
    final JPanel label = getMoreHintsLabel();
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.RELATIVE;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.insets = new Insets(0, 0, 1, 0);
    constraints.fill = GridBagConstraints.BOTH;
    constraints.ipady = 250;
    add(hintsPanel, constraints);

    constraints.weighty = 0;
    constraints.gridy = 1;
    constraints.ipady = 0;
    constraints.fill = GridBagConstraints.BOTH;
    add(label, constraints);
  }

  private JPanel getMoreHintsLabel() {
    final JPanel panel = new JPanel(new GridLayout(1, 1));
    final String text = "One more hint...";
    final JLabel moreHintsLabel = new JLabel(text);
    moreHintsLabel.addMouseListener(new HintsMouseListener(moreHintsLabel));
    moreHintsLabel.setHorizontalAlignment(SwingConstants.CENTER);
    moreHintsLabel.setOpaque(true);
    moreHintsLabel.setBackground(UIUtil.getTextFieldBackground());
    panel.add(moreHintsLabel);
    return panel;
  }

  private JScrollPane getHintsPanel(@NotNull final List<String> hints) {
    myHintPanel = new ScrollablePanel(new GridLayout(hints.size() * -1, 1));
    myHintPanel.setPreferredSize(new Dimension(600, 400));
    for (String hint : hints) {
      JLabel label = new JLabel(UIUtil.toHtml("<b>" + hint + "</b>", 5));
      myHintPanel.add(label);
      label.setVisible(false);
      label.setBorder(BorderFactory.createEtchedBorder());
      hintQueue.offer(label);
    }
    return ScrollPaneFactory.createScrollPane(myHintPanel);
  }

  private JLabel showNewHint() {
    final JLabel label = hintQueue.poll();
    label.setVisible(true);
    return label;
  }

  private class HintsMouseListener extends MouseAdapter {
    private final JLabel moreHintsLabel;

    public HintsMouseListener(@NotNull final JLabel moreHintsLabel) {
      this.moreHintsLabel = moreHintsLabel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (hintQueue.size() == 0) {
        BrowserUtil
          .browse(CheckIOUtils.getForumLink(myTask, myProject));
      }
      if (hintQueue.size() == 1) {
        moreHintsLabel
          .setText(UIUtil.toHtml("<a href=\"\"> Continue on forum...</a>"));
      }
      if (hintQueue.size() > 0) {
        JLabel label = showNewHint();
        myHintPanel.scrollRectToVisible(new Rectangle(label.getX(), label.getY(), label.getHeight(), label.getHeight()));
      }
    }
  }
}
