package com.jetbrains.checkio.ui;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.projectView.CourseDirectoryNode;
import com.jetbrains.edu.learning.projectView.StudyTreeStructureProvider;
import org.jetbrains.annotations.NotNull;


public class CheckIOTreeStructureProvider extends StudyTreeStructureProvider {

  @NotNull
  @Override
  protected CourseDirectoryNode createCourseNode(Project project, AbstractTreeNode node, ViewSettings settings, Course course) {
    if (course != null && course.getCourseType().equals(CheckIOBundle.message("check.io.course.type"))) {
      return new CheckIOCourseNode(project, ((PsiDirectory)node.getValue()), settings, course);
    }
    else {
      return super.createCourseNode(project, node, settings, course);    
    }
  }
}
