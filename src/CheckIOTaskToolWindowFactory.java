import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

public class CheckIOTaskToolWindowFactory implements ToolWindowFactory {
  private static final String TASK_DESCRIPTION = "Task description";
  private static final String SOLUTIONS = "Solutions";
  private static final String PROFILE = "Profile";

  private static Tree createSolutionsTree(String rootName) {
    final DefaultMutableTreeNode top = new DefaultMutableTreeNode(rootName);

    for (int i = 0; i < 5; i++) {
      top.add(new DefaultMutableTreeNode("Solutions " + (i + 1)));
    }
    Tree tree = new Tree(top);
    tree.setPreferredSize(new Dimension(400, 200));
    return tree;
  }

  private static JBList createList(String[] missionNames) {
    final DefaultListModel tasksListModel = new DefaultListModel();
    for (String name : missionNames) {
      tasksListModel.addElement(name);
    }

    return new JBList(tasksListModel);
  }


  private class TaskInfoPanel extends JPanel {
    public final JPanel myTaskDescriptionPanel;
    final JEditorPane myTaskDescriptionPane;
    public final JButton myShowSolutionButton;
    public final JButton myPublishSolutionButton;
    final JButton myViewProfileButton;
    final JPanel myButtonPanel;


    public TaskInfoPanel() {
      myTaskDescriptionPanel = new JPanel();
      myTaskDescriptionPane = new JEditorPane();
      myShowSolutionButton = new JButton("Show solutions");
      myTaskDescriptionPanel.setLayout(new BoxLayout(myTaskDescriptionPanel, BoxLayout.PAGE_AXIS));
      myPublishSolutionButton = new JButton("Publish solution");
      myViewProfileButton = new JButton("View Profile");
      myButtonPanel = new JPanel(new GridBagLayout());

      myTaskDescriptionPane.setPreferredSize(new Dimension(900, 900));
      myTaskDescriptionPane.setEditable(false);
      myPublishSolutionButton.setEnabled(false);

      myButtonPanel.add(myShowSolutionButton);
      myButtonPanel.add(myPublishSolutionButton);
      myButtonPanel.add(myViewProfileButton);

      myTaskDescriptionPanel.add(myTaskDescriptionPane);
      myTaskDescriptionPanel.add(myButtonPanel);
    }

    public void setTaskName(String taskName) {
      myTaskDescriptionPanel.add(new JBLabel(taskName));
    }

    public void setTaskDescription(String contentType, String taskDescription) {
      myTaskDescriptionPane.setContentType(contentType);
      myTaskDescriptionPane.setText(taskDescription);
    }
  }


  private static JPanel createTaskProgressPanel(String name, String[] itemNames) {
    final JPanel panel = new JPanel();
    //panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    final JBList completedTasksList = createList(itemNames);
    completedTasksList.setPreferredSize(new Dimension(200, 180));
    completedTasksList.add(new JBScrollBar());
    panel.add(completedTasksList);
    panel.setBorder(BorderFactory.createTitledBorder(name));
    return panel;
  }

  @Override
  public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
    // Panels used as cards in card layout
    final JBCardLayout cardLayout = new JBCardLayout();
    final JPanel contentPanel = new JPanel(cardLayout);
    final TaskInfoPanel taskDescriptionPanel = new TaskInfoPanel();


    final JPanel solutionsPanel = new JPanel();
    solutionsPanel.setLayout(new BoxLayout(solutionsPanel, BoxLayout.PAGE_AXIS));
    JPanel profilePanel = new JPanel();
    profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));


    //Constructing task description panel

    taskDescriptionPanel.myShowSolutionButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO);
      }
    });
    taskDescriptionPanel.myViewProfileButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, PROFILE, JBCardLayout.SwipeDirection.AUTO);
      }
    });


    // Constructing solutions panel
    final JButton toTaskDescription = new JButton(AllIcons.Actions.Back);
    toTaskDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
    final JPanel toTaskDescriptionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    toTaskDescriptionPanel.add(toTaskDescription);
    final JPanel clearSolutionsPanel = new JPanel();
    final JPanel creativeSolutionsPanel = new JPanel();
    final JPanel speedySolutionsPanel = new JPanel();
    clearSolutionsPanel.add(createSolutionsTree("Clear solutions"));
    creativeSolutionsPanel.add(createSolutionsTree("Creative solutions"));
    speedySolutionsPanel.add(createSolutionsTree("Speedy Solutions"));
    solutionsPanel.add(toTaskDescriptionPanel);
    solutionsPanel.add(clearSolutionsPanel);
    solutionsPanel.add(creativeSolutionsPanel);
    solutionsPanel.add(speedySolutionsPanel);


    //Constructing profile panel consisting of
    // infoPanel,
    // islandsProgressPanel,
    // tasksAndBadgesPanel
    final JPanel infoPanel = new JPanel(new GridBagLayout());
    final JPanel islandsProgressPanel = new JPanel(new GridLayout(3, 3, 10, 10));
    final JPanel tasksAndBadgesPanel = new JPanel(new GridLayout(1, 2, 10, 10));
    final JPanel tasksProgressPanel = new JPanel(new GridLayout(2, 1, 10, 10));
    final JPanel badgesPanel = new JPanel(new GridLayout());
    tasksAndBadgesPanel.add(tasksProgressPanel);
    tasksAndBadgesPanel.add(badgesPanel);

    GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 2, 1.0, 1.0,
                                                            GridBagConstraints.CENTER,
                                                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    infoPanel.add(new JLabel("1 badge"), constraints);
    constraints.gridx = 1;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    infoPanel.add(new JLabel("2 LVL"), constraints);
    constraints.gridx = 1;
    constraints.gridy = 1;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    final JProgressBar levelProgressBar = new JProgressBar(0, 45);
    levelProgressBar.setValue(25);
    constraints.anchor = GridBagConstraints.LAST_LINE_END;
    infoPanel.add(levelProgressBar, constraints);
    infoPanel.setBorder(BorderFactory.createTitledBorder("Sergei Python"));

    islandsProgressPanel.add(new JLabel("Home"));
    islandsProgressPanel.add(new JLabel("Elementary"));
    final JProgressBar progressBarIsland1 = new JProgressBar(0, 100);
    progressBarIsland1.setValue(3);

    islandsProgressPanel.add(progressBarIsland1);
    final JProgressBar progressBarIsland2 = new JProgressBar(0, 100);
    progressBarIsland2.setValue(5);
    islandsProgressPanel.add(progressBarIsland2);
    islandsProgressPanel.setBorder(BorderFactory.createTitledBorder("Progress"));

    tasksProgressPanel.add(createTaskProgressPanel("Missions", new String[]{"Median", "Fizz Buzz"}));
    tasksProgressPanel.add(createTaskProgressPanel("Publications", new String[]{"First"}));

    ImageIcon badgeIcon = createImageIcon("/resources/good_start.png");
    final JLabel badgeLabel = new JLabel(badgeIcon);
    badgesPanel.setLayout(new BoxLayout(badgesPanel, BoxLayout.PAGE_AXIS));
    badgesPanel.add(badgeLabel);
    badgesPanel.setBorder(BorderFactory.createTitledBorder("Badges"));

    final JButton backFromProfileButton = new JButton(AllIcons.Actions.Back);
    final JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    backButtonPanel.add(backFromProfileButton);
    profilePanel.add(backButtonPanel);
    profilePanel.add(infoPanel);
    profilePanel.add(islandsProgressPanel);
    profilePanel.add(tasksAndBadgesPanel);

    contentPanel.add(TASK_DESCRIPTION, taskDescriptionPanel);
    contentPanel.add(SOLUTIONS, solutionsPanel);
    contentPanel.add(PROFILE, profilePanel);

    final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    final Content content = contentFactory.createContent(contentPanel, "", true);
    toolWindow.getContentManager().addContent(content);

    toTaskDescription.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
      }
    });
    showSolutionButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO);
      }
    });
    viewProfileButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, PROFILE, JBCardLayout.SwipeDirection.AUTO);
      }
    });

    backFromProfileButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
      }
    });
  }

  private ImageIcon createImageIcon(String path) {
    URL imgURL = getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    }
    else {
      //System.err.println("Couldn't find file: " + path);
      return null;
    }
  }
}
