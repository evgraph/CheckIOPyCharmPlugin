package com.jetbrains.checkio.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.JBColor;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.projectView.TaskDirectoryNode;
import icons.InteractiveLearningIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;


class CheckIOStudyNode extends TaskDirectoryNode {

  private final Task myTask;
  private final Project myProject;

  public CheckIOStudyNode(@NotNull Project project,
                          PsiDirectory value,
                          ViewSettings viewSettings,
                          @NotNull Task task) {
    super(project, value, viewSettings, task);
    myProject = project;
    myTask = task;
  }

  @Nullable
  @Override
  public AbstractTreeNode modifyChildNode(AbstractTreeNode node) {
    return null;
  }

  @Override
  public PsiDirectoryNode createChildDirectoryNode(StudyItem item, PsiDirectory directory) {
    return null;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    StudyStatus taskStatus = myTask.getStatus();
    final String name = myTask.getName();
    switch (taskStatus) {
      case Unchecked: {
        updatePresentation(data, name, JBColor.BLACK, InteractiveLearningIcons.Task, null);
        break;
      }
      case Solved: {
        final boolean published = CheckIOTaskManager.getInstance(myProject).isPublished(myTask);
        updatePresentation(data, published ? name + "[published]" : name, new JBColor(new Color(0, 134, 0), new Color(98, 150, 85)),
                           InteractiveLearningIcons.TaskCompl, null);
        break;
      }
      case Failed: {
        updatePresentation(data, name, JBColor.RED, InteractiveLearningIcons.TaskProbl, null);
      }
    }
  }
}
