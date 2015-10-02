package com.jetbrains.checkio.ui;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.projectView.StudyTreeStructureProvider;
import org.jetbrains.annotations.NotNull;


public class CheckIOTreeStructureProvider extends StudyTreeStructureProvider {

  @NotNull
  @Override
  protected AbstractTreeNode createStudyDirectoryNode(ViewSettings settings, Project project, PsiDirectory nodeValue) {
    return new CheckIOStudyNode(project, nodeValue, settings);
  }
}
