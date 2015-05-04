import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
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
  private static final DefaultLogger LOG = new DefaultLogger(CheckIOTaskToolWindowFactory.class.getName());

  @Override
  public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
    final JBCardLayout cardLayout = new JBCardLayout();
    final JPanel contentPanel = new JPanel(cardLayout);

    final TaskInfoPanel taskInfoPanel = new TaskInfoPanel();
    final SolutionsPanel solutionsPanel = new SolutionsPanel();
    final ProfilePanel profilePanel = new ProfilePanel(project);

    contentPanel.add(TASK_DESCRIPTION, taskInfoPanel);
    contentPanel.add(SOLUTIONS, solutionsPanel);
    contentPanel.add(PROFILE, profilePanel);

    final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    final Content content = contentFactory.createContent(contentPanel, "", true);
    toolWindow.getContentManager().addContent(content);

    taskInfoPanel.getShowSolutionsButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO);
      }
    });

    taskInfoPanel.getViewProfileButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, PROFILE, JBCardLayout.SwipeDirection.AUTO);
      }
    });

    solutionsPanel.toTaskDescription.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
      }
    });


    profilePanel.getBackFromProfileButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
      }
    });
  }

  private static class SolutionsPanel extends JPanel {
    private static final String CLEAR_SOLUTIONS = "Clear solutions";
    private static final String CREATIVE_SOLUTIONS = "Creative solutions";
    private static final String SPEEDY_SOLUTIONS = "Speedy solutions";
    private JPanel clearSolutionsPanel;
    private JPanel creativeSolutionsPanel;
    private JPanel speedySolutionsPanel;
    private JButton toTaskDescription = new JButton(AllIcons.Actions.Back);

    public SolutionsPanel() {
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      clearSolutionsPanel = createSolutionsPanel(CLEAR_SOLUTIONS, null);
      creativeSolutionsPanel = createSolutionsPanel(CREATIVE_SOLUTIONS, null);
      speedySolutionsPanel = createSolutionsPanel(SPEEDY_SOLUTIONS, null);
      add(clearSolutionsPanel);
      add(creativeSolutionsPanel);
      add(speedySolutionsPanel);
      add(createButtonPanel());
      add(createButtonPanel());
    }

    private static JPanel createSolutionsPanel(String panelName, String[] solutionsNames) {
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

    private JPanel createButtonPanel() {
      JPanel buttonPanel = new JPanel();
      toTaskDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
      buttonPanel.add(toTaskDescription);
      return buttonPanel;
    }
  }

  static class ProfilePanel extends JPanel {
    private final static int USER_PROGRESS = 25;
    final JButton backFromProfileButton = new JButton(AllIcons.Actions.Back);
    private InfoPanel infoPanel;
    private StationsProgressPanel myStationsProgressPanel;
    private TaskAndBadgesProgressPanel tasksAndBadgesPanel;
    private JPanel backButtonPanel;
    private CheckIOUser user;

    public ProfilePanel(Project project) {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      user = CheckIOTaskManager.getInstance(project).getUser();
      backButtonPanel = createBackButtonPanel();
      infoPanel = new InfoPanel();
      myStationsProgressPanel = new StationsProgressPanel("Home", "Elementary");
      tasksAndBadgesPanel = new TaskAndBadgesProgressPanel();
      add(backButtonPanel);
      add(infoPanel);
      add(myStationsProgressPanel);
      add(tasksAndBadgesPanel);
    }

    private static JProgressBar setProgressBar(int value, int to) {
      JProgressBar progressBar = new JProgressBar(0, 45);
      progressBar.setValue(value);
      return progressBar;
    }

    private static GridBagConstraints setConstraints(int gridx, int gridy, int gridwidth, int gridheigh) {
      final GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 2, 1.0, 1.0,
                                                                    GridBagConstraints.CENTER,
                                                                    GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      constraints.gridx = gridx;
      constraints.gridy = gridy;
      constraints.gridwidth = gridwidth;
      constraints.gridheight = gridheigh;

      return constraints;
    }

    public JButton getBackFromProfileButton() {
      return backFromProfileButton;
    }

    private JPanel createBackButtonPanel() {

      final JPanel backButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      backButtonPanel.add(backFromProfileButton);
      return backButtonPanel;
    }

    static class TaskAndBadgesProgressPanel extends JPanel {
      private TaskProgressPanel myTaskProgressPanel;
      private BadgesPanel myBadgesPanel;

      public TaskAndBadgesProgressPanel() {
        setLayout(new GridLayout(1, 2, 10, 10));
        myTaskProgressPanel = new TaskProgressPanel();
        myBadgesPanel = new BadgesPanel();
        add(myTaskProgressPanel);
        add(myBadgesPanel);
      }

      static class TaskProgressPanel extends JPanel {
        private JPanel missionsPanel;
        private JPanel publicationsPanel;

        public TaskProgressPanel() {
          setLayout(new GridLayout(2, 1, 10, 10));
          missionsPanel = createListPanel("Missions", new String[]{""});
          publicationsPanel = createListPanel("Publications", new String[]{""});
          add(missionsPanel);
          add(publicationsPanel);
        }

        private static JPanel createListPanel(String name, String[] itemNames) {
          final JPanel panel = new JPanel();
          final JBList completedTasksList = createList(itemNames);
          completedTasksList.setPreferredSize(new Dimension(200, 180));
          completedTasksList.add(new JBScrollBar());
          panel.add(completedTasksList);
          panel.setBorder(BorderFactory.createTitledBorder(name));
          return panel;
        }


        private static JBList createList(String[] missionNames) {
          final DefaultListModel<String> tasksListModel = new DefaultListModel<>();
          for (String name : missionNames) {
            tasksListModel.addElement(name);
          }

          return new JBList(tasksListModel);
        }
      }

      static class BadgesPanel extends JPanel {
        private ImageIcon badgeIcon;
        private JLabel badgeLabel;

        public BadgesPanel() {
          badgeIcon = createImageIcon("/resources/good_start.png");
          badgeLabel = new JLabel(badgeIcon);
          setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
          add(badgeLabel);
          setBorder(BorderFactory.createTitledBorder("Badges"));
        }

        private ImageIcon createImageIcon(String path) {
          final URL imgURL = getClass().getResource(path);
          if (imgURL != null) {
            return new ImageIcon(imgURL);
          }
          else {
            LOG.warn("Couldn't find path: " + path);
            return null;
          }
        }
      }
    }

    class InfoPanel extends JPanel {
      private JLabel myBadgesLabel;
      private JLabel myLevelLabel;
      private JProgressBar myLevelProgressBar;

      public InfoPanel() {
        setLayout(new GridBagLayout());
        myBadgesLabel = new JLabel("1 badge");
        myLevelLabel = new JLabel(user.getLevel() + " LVL");
        GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 2, 1.0, 1.0,
                                                                GridBagConstraints.CENTER,
                                                                GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
        add(myBadgesLabel, constraints);
        add(myLevelLabel, setConstraints(1, 0, 1, 1));
        constraints = setConstraints(1, 1, 1, 1);
        constraints.anchor = GridBagConstraints.LAST_LINE_END;
        add(myLevelProgressBar = setProgressBar(USER_PROGRESS, 45), constraints);
        setBorder(BorderFactory.createTitledBorder(user.getUsername()));
      }
    }

    class StationsProgressPanel extends JPanel {
      private JLabel firstStationLabel;
      private JLabel secondStationLabel;

      public StationsProgressPanel(String firstStationName, String secondStationName) {
        setLayout(new GridLayout(3, 3, 10, 10));
        firstStationLabel = new JLabel(firstStationName);
        secondStationLabel = new JLabel(secondStationName);
        add(firstStationLabel);
        add(secondStationLabel);
        add(setProgressBar(3, 100));
        add(setProgressBar(5, 100));
        setBorder(BorderFactory.createTitledBorder("Progress"));
      }
    }
  }

  private class TaskInfoPanel extends JPanel {
    private JEditorPane myTaskTextPane;
    private ButtonPanel myButtonPanel;
    private JLabel taskNameLabel;

    public TaskInfoPanel() {
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      myButtonPanel = new ButtonPanel();
      myTaskTextPane = makeTaskTextPane();
      taskNameLabel = new JLabel();
      setTaskNameLabelText("a");
      add(taskNameLabel);
      add(myTaskTextPane);
      add(myButtonPanel);
    }

    public JButton getShowSolutionsButton() {
      return myButtonPanel.myShowSolutionButton;
    }

    public JButton getViewProfileButton() {
      return myButtonPanel.myViewProfileButton;
    }

    public JButton getPublishSolutionsButton() {
      return myButtonPanel.myPublishSolutionButton;
    }

    private JEditorPane makeTaskTextPane() {
      JEditorPane taskTextPane = new JEditorPane();
      taskTextPane.setPreferredSize(new Dimension(900, 900));
      taskTextPane.setEditable(false);
      return taskTextPane;
    }

    public void setTaskText(String contentType, String taskDescription) {
      myTaskTextPane.setContentType(contentType);
      myTaskTextPane.setText(taskDescription);
    }

    public void setTaskNameLabelText(String taskName) {
      this.taskNameLabel.setText(taskName);
    }

    private class ButtonPanel extends JPanel {
      private static final String SHOW_SOLUTIONS_BUTTON_TEXT = "Show solutions";
      private static final String PUBLISH_SOLUTIONS_BUTTON_TEXT = "Publish solution";
      private static final String VIEW_PROFILE_BUTTON_TEXT = "View Profile";
      private JButton myShowSolutionButton;
      private JButton myPublishSolutionButton;
      private JButton myViewProfileButton;


      public ButtonPanel() {
        initButtons();
        add(myPublishSolutionButton);
        add(myShowSolutionButton);
        add(myViewProfileButton);
      }

      private void initButtons() {
        myShowSolutionButton = new JButton(SHOW_SOLUTIONS_BUTTON_TEXT);
        myPublishSolutionButton = new JButton(PUBLISH_SOLUTIONS_BUTTON_TEXT);
        myViewProfileButton = new JButton(VIEW_PROFILE_BUTTON_TEXT);
        myPublishSolutionButton.setEnabled(false);
        myShowSolutionButton.setEnabled(false);
      }
    }
  }
}
