package com.jetbrains.checkio.ui;


import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.ui.StudyProgressBar;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class CheckIOUserInfoToolWindowFactory implements ToolWindowFactory {
  @Override
  public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow window) {
    final JPanel contentPanel = new JPanel();
    contentPanel.setPreferredSize(new Dimension(100, 300));
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    final CheckIOTaskManager checkIOTaskManager = CheckIOTaskManager.getInstance(project);
    final Course course = studyTaskManager.getCourse();
    final CheckIOUser user = checkIOTaskManager.getUser();
    if (course != null && user != null) {
      contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
      contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
      final JLabel userNameLabel = new JLabel(UIUtil.toHtml("<b>User: </b>" + "<a href=\"\">" + user.getUsername() + "</a>", 5));
      userNameLabel.addMouseListener(new MyMouseListener(user));
      final JLabel userLevelLabel = new JLabel(UIUtil.toHtml("<b>Level: </b>" + user.getLevel(), 5));
      contentPanel.add(userNameLabel);
      contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
      contentPanel.add(userLevelLabel);
      contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
      int taskNum = 0;
      int taskSolved = 0;
      int lessonsCompleted = 0;
      List<Lesson> lessons = course.getLessons();

      for (Lesson lesson : lessons) {
        taskNum += lesson.getTaskList().size();
        taskSolved += getSolvedTasks(lesson, studyTaskManager);
        lessonsCompleted += studyTaskManager.getStatus(lesson) == StudyStatus.Solved ? 1 : 0;
      }


      String completedLessons = String.format("%d of %d lessons completed", lessonsCompleted, course.getLessons().size());
      String completedTasks = String.format("%d of %d tasks completed", taskSolved, taskNum);
      String tasksLeft = String.format("%d of %d tasks left", taskNum - taskSolved, taskNum);
      contentPanel.add(Box.createVerticalStrut(10));
      addStatistics(completedLessons, contentPanel);
      addStatistics(completedTasks, contentPanel);

      double percent = (taskSolved * 100.0) / taskNum;
      contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
      StudyProgressBar studyProgressBar = new StudyProgressBar(percent / 100, 40, 10);
      contentPanel.add(studyProgressBar);
      addStatistics(tasksLeft, contentPanel);
      ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
      Content content = contentFactory.createContent(contentPanel, "", true);
      window.getContentManager().addContent(content);
    }
  }

  private static int getSolvedTasks(@NotNull final Lesson lesson, @NotNull final StudyTaskManager taskManager) {
    int solved = 0;
    List<Task> tasks = lesson.getTaskList();

    for (Task task : tasks) {
      if (taskManager.getStatus(task) == StudyStatus.Solved) {
        ++solved;
      }
    }

    return solved;
  }

  private static void addStatistics(String statistics, JPanel contentPanel) {
    String labelText = UIUtil.toHtml(statistics, 5);
    contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    JLabel statisticLabel = new JLabel(labelText);
    contentPanel.add(statisticLabel);
  }

  private class MyMouseListener implements MouseListener {
    private CheckIOUser myUser;

    public MyMouseListener(@NotNull final CheckIOUser user) {
      myUser = user;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      BrowserUtil.browse(CheckIOUtils.getUserProfileLink(myUser));
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
