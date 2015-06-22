package taskPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollBar;
import com.jetbrains.edu.courseFormat.Task;
import main.CheckIOTaskManager;
import main.CheckIOUser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;

public class ProfilePanel extends JPanel {
  private final static int USER_PROGRESS = 25;
  private static final DefaultLogger LOG = new DefaultLogger(ProfilePanel.class.getName());
  final JButton backFromProfileButton = new JButton(AllIcons.Actions.Back);

  public ProfilePanel(Project project) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));


    addAll(createBackButtonPanel(), new InfoPanel(project),
           new StationsProgressPanel("Home", "Elementary"), new TaskAndBadgesProgressPanel(project));


  }

  private static JProgressBar setProgressBar(int value, int to) {
    JProgressBar progressBar = new JProgressBar(0, to);
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

  private void addAll(JPanel backButtonPanel, InfoPanel infoPanel, StationsProgressPanel stationsProgressPanel,
                      TaskAndBadgesProgressPanel tasksAndBadgesPanel) {
    add(backButtonPanel);
    add(infoPanel);
    add(stationsProgressPanel);
    add(tasksAndBadgesPanel);

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

    public TaskAndBadgesProgressPanel(@NotNull Project project) {
      setLayout(new GridLayout(1, 2, 10, 10));
      myTaskProgressPanel = new TaskProgressPanel(project);
      myBadgesPanel = new BadgesPanel();
      add(myTaskProgressPanel);
      add(myBadgesPanel);
    }

    static class TaskProgressPanel extends JPanel {
      private JPanel missionsPanel;
      private JPanel publicationsPanel;

      public TaskProgressPanel(@NotNull Project project) {
        setLayout(new GridLayout(2, 1, 10, 10));
        missionsPanel = createListPanel("Missions", project);
        publicationsPanel = createListPanel("Publications", project);
        add(missionsPanel);
        add(publicationsPanel);
      }


      private static JPanel createListPanel(@NotNull String name, @NotNull Project project) {
        final JPanel panel = new JPanel();
        final JBList completedTasksList = createList(project);
        completedTasksList.setPreferredSize(new Dimension(200, 180));
        completedTasksList.add(new JBScrollBar());
        panel.add(completedTasksList);
        panel.setBorder(BorderFactory.createTitledBorder(name));
        return panel;
      }


      private static JBList createList(@NotNull Project project) {
        final DefaultListModel<String> tasksListModel = new DefaultListModel<>();
        ArrayList<Task> publishedTasks = CheckIOTaskManager.getInstance(project).getPublishedTasks();
        for (Task task : publishedTasks) {
          tasksListModel.addElement(task.getName());
        }
        for (int i = 0; i < 10; i++) {
          tasksListModel.addElement("sss");
        }
        JBList list = new JBList(tasksListModel);
        list.add(new JBScrollBar());
        return list;
      }


      }

    //  class MyPublicationListener implements ListSelectionListener{
    //    @Override
    //    public void valueChanged(ListSelectionEvent e) {
    //      e.getSource()
    //    }
    //  }
    //}

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

  static class StationsProgressPanel extends JPanel {
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

  static class InfoPanel extends JPanel {
    private JLabel myBadgesLabel;
    private JLabel myLevelLabel;
    private JProgressBar myLevelProgressBar;

    public InfoPanel(@NotNull Project project) {
      CheckIOTaskManager manager = CheckIOTaskManager.getInstance(project);
      CheckIOUser user = manager.getUser();
      assert user != null;
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
      myLevelProgressBar = setProgressBar(USER_PROGRESS, 45);
      add(myLevelProgressBar, constraints);
      setBorder(BorderFactory.createTitledBorder(user.getUsername()));
    }
  }
}