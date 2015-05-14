import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollBar;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

class ProfilePanel extends JPanel {
  private final static int USER_PROGRESS = 25;
  private static final DefaultLogger LOG = new DefaultLogger(ProfilePanel.class.getName());
  final JButton backFromProfileButton = new JButton(AllIcons.Actions.Back);
  private InfoPanel infoPanel;
  private StationsProgressPanel myStationsProgressPanel;
  private TaskAndBadgesProgressPanel tasksAndBadgesPanel;
  private JPanel backButtonPanel;
  private CheckIOUser user;


  public ProfilePanel(Project project) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    CheckIOTaskManager manager = CheckIOTaskManager.getInstance(project);
    user = manager.getUser();
    //user = new CheckIOUser("test", 1, 1);
    if (user == null) {
      LOG.error("Create ToolWindow Content: user is null");
      return;
    }
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
      myLevelProgressBar = setProgressBar(USER_PROGRESS, 45);
      add(myLevelProgressBar, constraints);
      setBorder(BorderFactory.createTitledBorder(user.getUsername()));
    }
  }
}