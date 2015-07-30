package com.jetbrains.checkio.ui;


import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
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

public class CheckIOHintToolWindowFactory implements ToolWindowFactory, DumbAware {
  public static final String ID = "Hints";
  private LinkedBlockingQueue<JLabel> hintQueue = new LinkedBlockingQueue<>();
  private static final Logger LOG = Logger.getInstance(CheckIOHintToolWindowFactory.class);
  private Task myTask;
  private Project myProject;

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow window) {
    myTask = CheckIOUtils.getTaskFromSelectedEditor(project);
    myProject = project;

    if (myTask == null) {
      LOG.warn("User request hints for an empty editor");
      ToolWindowManager.getInstance(project).unregisterToolWindow(ID);
      return;
    }
    //TODO: change (api needed)
    //final ArrayList<String> hints = taskManager.myTaskHints.get(myTask.getName());
    final List<String> hints = Arrays.asList("Try to run the code and see which test fails", "The problem is likely an empty dictionary.",
                                             "Try to add special case processing.",
                                             "Look carefully when your code checks keys and values in the \"for ... in ...\" loop.",
                                             "You can replace a value if it equals {}",

                                             "Try to run the code and see which test fails", "The problem is likely an empty dictionary.",
                                             "Try to add special case processing.",
                                             "Look carefully when your code checks keys and values in the \"for ... in ...\" loop.",
                                             "You can replace a value if it equals {}");

    final JPanel contentPanel = new JPanel(new GridBagLayout());
    final JScrollPane hintsPanel = getHintsPanel(hints);
    contentPanel.setMinimumSize(new Dimension(0, 0));
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
    contentPanel.add(hintsPanel, constraints);

    constraints.weighty = 0;
    constraints.gridy = 1;
    constraints.ipady = 0;
    constraints.fill = GridBagConstraints.BOTH;
    contentPanel.add(label, constraints);
    showNewHint();

    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    Content content = contentFactory.createContent(contentPanel, "", true);
    window.getContentManager().addContent(content);
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
    final ScrollablePanel hintPanel = new ScrollablePanel(new GridLayout(hints.size() * -1, 1));
    hintPanel.setPreferredSize(new Dimension(600, 400));
    for (String hint : hints) {
      JLabel label = new JLabel(UIUtil.toHtml("<b>" + hint + "</b>", 5));
      hintPanel.add(label);
      label.setVisible(false);
      label.setBorder(BorderFactory.createEtchedBorder());
      hintQueue.offer(label);
    }
    return ScrollPaneFactory.createScrollPane(hintPanel);
  }

  private void showNewHint() {
    hintQueue.poll().setVisible(true);
  }

  private class HintsMouseListener extends MouseAdapter {
    private JLabel moreHintsLabel;

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
        showNewHint();
      }
    }
  }
}
