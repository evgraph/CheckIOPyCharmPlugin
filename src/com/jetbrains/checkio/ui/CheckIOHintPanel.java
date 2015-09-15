package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
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
  private ScrollablePanel myHintsPanel;

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
    myHintsPanel = getHintsPanel(hints);
    JPanel closeButtonPanel = createCloseLabelPanel();

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.RELATIVE;
    constraints.weightx = 1;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.ipady = 0;
    add(closeButtonPanel, constraints);

    constraints.gridy = 1;
    constraints.weighty = 1;
    constraints.insets = new Insets(0, 0, 1, 0);
    add(new JBScrollPane(myHintsPanel), constraints);

    setPreferredSize(getPreferredSize());
  }

  private JLabel createContinueOnForumLabel() {
    final JLabel moreHintsLabel = new JLabel(UIUtil.toHtml("<b> Continue on forum... </b>"));
    moreHintsLabel.setBorder(BorderFactory.createEtchedBorder(UIUtil.getLabelBackground(), UIUtil.getLabelBackground()));
    moreHintsLabel.addMouseListener(new HintsMouseListener());
    moreHintsLabel.setHorizontalAlignment(SwingConstants.CENTER);
    moreHintsLabel.setToolTipText("Click to open forum on web");
    moreHintsLabel.setForeground(UIUtil.getLabelBackground());
    return moreHintsLabel;
  }

  private JPanel createCloseLabelPanel() {
    final JLabel closeIconLabel = new JLabel(AllIcons.Actions.Close);
    final JLabel titleLabel = new JLabel("Hints");
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEtchedBorder());
    closeIconLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        myCheckIOToolWindow.hideHintPanel();
      }
    });

    panel.add(titleLabel, BorderLayout.WEST);
    panel.add(closeIconLabel, BorderLayout.EAST);
    return panel;
  }

  private ScrollablePanel getHintsPanel(@NotNull final List<String> hints) {
    final ScrollablePanel panel = new ScrollablePanel(new GridBagLayout());

    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.RELATIVE;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1;
    constraints.gridx = 0;

    for (int i = 0; i < hints.size(); i++) {
      final String hint = hints.get(i);
      final JLabel label = new JLabel(UIUtil.toHtml("<br> <b>" + hint + "</b> <br> ", 5));
      label.setForeground(UIUtil.getLabelBackground());
      hintQueue.offer(label);

      constraints.gridy = i;
      panel.add(label, constraints);
    }

    final JLabel continueOnForumLabel = createContinueOnForumLabel();
    hintQueue.offer(continueOnForumLabel);

    constraints.gridy = hintQueue.size() - 1;
    panel.add(continueOnForumLabel, constraints);

    return panel;
  }

  public void showNewHint() {
    if (!hintQueue.isEmpty()) {
      final JLabel label = hintQueue.poll();

      if (hintQueue.isEmpty()) {
        label.setForeground(UIUtil.getListSelectionBackground());
      }
      else {
        label.setForeground(UIUtil.getLabelForeground());
      }

      label.setBorder(BorderFactory.createEtchedBorder());

      final JComponent parent = (JComponent)myHintsPanel.getParent();
      if (parent != null) {
        parent.scrollRectToVisible(label.getBounds());
      }
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
