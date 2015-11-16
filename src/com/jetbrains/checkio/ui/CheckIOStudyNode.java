package com.jetbrains.checkio.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.JBColor;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.edu.courseFormat.StudyStatus;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.projectView.StudyDirectoryNode;
import icons.EducationalIcons;
import icons.InteractiveLearningIcons;
import org.jetbrains.annotations.NotNull;

import java.awt.*;


class CheckIOStudyNode extends StudyDirectoryNode {
  public CheckIOStudyNode(@NotNull Project project,
                          PsiDirectory value,
                          ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  protected void setStudyAttributes(Task task, PresentationData data, String additionalName) {
    StudyStatus taskStatus = StudyTaskManager.getInstance(myProject).getStatus(task);
    switch (taskStatus) {
      case Unchecked: {
        updatePresentation(data, additionalName, JBColor.BLACK, EducationalIcons.Task);
        break;
      }
      case Solved: {
        final boolean published = CheckIOTaskManager.getInstance(myProject).isPublished(task);
        additionalName = published ? additionalName + "[published]" : additionalName;
        updatePresentation(data, additionalName, new JBColor(new Color(0, 134, 0), new Color(98, 150, 85)),
                           InteractiveLearningIcons.TaskCompl);
        break;
      }
      case Failed: {
        updatePresentation(data, additionalName, JBColor.RED, InteractiveLearningIcons.TaskProbl);
      }
    }
  }
}
