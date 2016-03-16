package com.jetbrains.checkio.ui;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.projectView.StudyTreeStructureProvider;
import org.jetbrains.annotations.NotNull;


public class CheckIOTreeStructureProvider extends StudyTreeStructureProvider {

  @NotNull
  @Override
  protected AbstractTreeNode createStudyDirectoryNode(ViewSettings settings, Project project, PsiDirectory nodeValue) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course != null && course.getCourseType().equals(CheckIOBundle.message("check.io.course.type"))) {
      return new CheckIOStudyNode(project, nodeValue, settings);
    }
    else {
      return super.createStudyDirectoryNode(settings, project, nodeValue);
    }
  }
}
