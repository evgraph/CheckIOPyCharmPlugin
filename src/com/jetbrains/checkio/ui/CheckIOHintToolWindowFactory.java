package com.jetbrains.checkio.ui;


import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class CheckIOHintToolWindowFactory implements ToolWindowFactory, DumbAware {
  public static final String ID = "Hints";
  private LinkedBlockingQueue<JLabel> hintQueue = new LinkedBlockingQueue<>();
  private static final Logger LOG = Logger.getInstance(CheckIOHintToolWindowFactory.class);

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow window) {
    final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);

    if (task == null) {
      LOG.warn("User request hints for an empty editor");
      ToolWindowManager.getInstance(project).unregisterToolWindow(ID);
      return;
    }
    //final ArrayList<String> hints = taskManager.myTaskHints.get(task.getName());
    final List<String> hints = Arrays.asList("Try to run the code and see which test fails", "The problem is likely an empty dictionary.",
                                             "Try to add special case processing.",
                                             "Look carefully when your code checks keys and values in the \"for ... in ...\" loop.",
                                             "You can replace a value if it equals {}",

                                             "Try to run the code and see which test fails", "The problem is likely an empty dictionary.",
                                             "Try to add special case processing.",
                                             "Look carefully when your code checks keys and values in the \"for ... in ...\" loop.",
                                             "You can replace a value if it equals {}");

    final JPanel contentPanel = new JPanel(new GridBagLayout());
    final JPanel hintsPanel = getHintsPanel(hints);
    final JPanel label = getMoreHintsLabel();
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = 2;
    constraints.gridheight = 1;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.insets = new Insets(0, 0, 1, 0);
    constraints.ipady = 250;

    contentPanel.add(hintsPanel, constraints);
    constraints.gridy = 1;
    constraints.ipady = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
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

  private JPanel getHintsPanel(@NotNull final List<String> hints) {
    final JPanel hintPanel = new JPanel(new GridLayout(hints.size() * -1, 1));
    hintPanel.setSize(400, 400);
    for (String hint : hints) {
      JLabel label = new JLabel(UIUtil.toHtml("<b>" + hint + "</b>", 5));
      hintPanel.add(label);
      label.setVisible(false);
      label.setBorder(BorderFactory.createEtchedBorder());
      hintQueue.offer(label);
    }
    return hintPanel;
  }

  private void showNewHint() {
    hintQueue.poll().setVisible(true);
  }

  private class HintsMouseListener implements MouseListener {
    private JLabel moreHintsLabel;

    public HintsMouseListener(@NotNull final JLabel moreHintsLabel) {
      this.moreHintsLabel = moreHintsLabel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (hintQueue.size() == 0) {
        BrowserUtil
          .browse("http://www.checkio.org/forum/add/?source=hints&task_id=325&interpreter=python-27&q=tag%3Afor_advisers,hint.bryukh");
      }
      if (hintQueue.size() == 1) {
        moreHintsLabel
          .setText(UIUtil.toHtml("<a class=\"hints__group__help__forum\" target=\"_blank\" href=\"/forum/add/?source=hints&amp;" +
                                 "task_id=325&amp;interpreter=python-27&amp;q=tag%3Afor_advisers,hint.bryukh\">" +
                                 "Continue on forum...</a>"));
      }
      if (hintQueue.size() > 0) {
        showNewHint();
      }
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
}
