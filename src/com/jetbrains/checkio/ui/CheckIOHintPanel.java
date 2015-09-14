package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
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
  private final CheckIOToolWindow myCheckIOToolWindow;
  private Task myTask;
  private Project myProject;
  private ScrollablePanel myHintPanel;
  private JPanel myForumPanel;

  public CheckIOHintPanel(@NotNull final Project project, @NotNull final CheckIOToolWindow toolWindow) {
    myCheckIOToolWindow = toolWindow;
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
    myForumPanel = createContinueOnForumPanel();
    JPanel closeButtonPanel = createCloseLabelPanel();

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.RELATIVE;
    constraints.weightx = 1;
    constraints.weighty = 0;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.ipady = 0;
    add(closeButtonPanel, constraints);

    constraints.gridy = 1;
    constraints.weighty = 1;
    constraints.ipady = 250;
    constraints.insets = new Insets(0, 0, 1, 0);
    add(hintsPanel, constraints);

    constraints.weighty = 0;
    constraints.gridy = 2;
    constraints.ipady = 0;
    constraints.fill = GridBagConstraints.BOTH;
    add(myForumPanel, constraints);
  }

  private JPanel createContinueOnForumPanel() {
    final JPanel panel = new JPanel(new GridLayout(1, 1));
    final JLabel moreHintsLabel = new JLabel(UIUtil.toHtml("<a href=\"\"> Continue on forum...</a>"));
    moreHintsLabel.addMouseListener(new HintsMouseListener());
    moreHintsLabel.setHorizontalAlignment(SwingConstants.CENTER);
    moreHintsLabel.setOpaque(true);
    moreHintsLabel.setBackground(UIUtil.getTextFieldBackground());
    panel.add(moreHintsLabel);
    panel.setVisible(false);
    return panel;
  }

  private JPanel createCloseLabelPanel() {
    final JLabel label = new JLabel(AllIcons.Actions.Close);
    final JPanel panel = new JPanel(new BorderLayout());
    label.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        myCheckIOToolWindow.hideHintPanel();
      }
    });
    panel.add(label, BorderLayout.EAST);
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

  public void showNewHint() {
    final JLabel label = hintQueue.poll();
    label.setVisible(true);
    scrollRectToVisible(new Rectangle(label.getX(), label.getY(), label.getHeight(), label.getHeight()));

    if (hintQueue.isEmpty()) {
      myForumPanel.setVisible(true);
    }
  }

  private class HintsMouseListener extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent e) {
      if (hintQueue.size() == 0) {
        BrowserUtil
          .browse(CheckIOUtils.getForumLink(myTask, myProject));
      }
    }
  }
}
