package com.jetbrains.checkio.ui;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.projectView.CourseDirectoryNode;
import com.jetbrains.edu.learning.projectView.LessonDirectoryNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CheckIOCourseNode extends CourseDirectoryNode {
  public CheckIOCourseNode(@NotNull Project project,
                           PsiDirectory value,
                           ViewSettings viewSettings,
                           @NotNull Course course) {
    super(project, value, viewSettings, course);
  }

  @Override
  public PsiDirectoryNode createChildDirectoryNode(StudyItem item, PsiDirectory directory) {
    final List<Lesson> lessons = myCourse.getLessons();
    final Lesson lesson = (Lesson)item;
    if (directory.getChildren().length > 0 && lessons.size() == 1) {
      final List<Task> tasks = lesson.getTaskList();
      if (tasks.size() == 1) {
        PsiDirectory taskDirectory = (PsiDirectory)directory.getChildren()[0];
        PsiDirectory srcDir = taskDirectory.findSubdirectory(EduNames.SRC);
        if (srcDir != null) {
          taskDirectory = srcDir;
        }
        return new CheckIOStudyNode(myProject, taskDirectory, myViewSettings, tasks.get(0));
      }
    }
    return new LessonDirectoryNode(myProject, directory, myViewSettings, lesson);
  }
}
