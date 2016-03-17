package com.jetbrains.checkio;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.PlatformUtils;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CheckIOInterpreterInspection extends PyInspection {

  @Nls
  @NotNull
  public String getDisplayName() {
    return CheckIOBundle.message("inspection.language.level.mismatch.name");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                        final boolean isOnTheFly,
                                        @NotNull final LocalInspectionToolSession session) {
    return new Visitor(holder, session);
  }

  public static class Visitor extends PyInspectionVisitor {

    public Visitor(@Nullable ProblemsHolder holder,
                   @NotNull LocalInspectionToolSession session) {
      super(holder, session);
    }

    @Override
    public void visitPyFile(PyFile node) {
      super.visitPyFile(node);
      if (PlatformUtils.isPyCharm()) {
        final Module module = ModuleUtilCore.findModuleForPsiElement(node);
        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(node);
        if (module != null && virtualFile != null) {
          final LanguageLevel publicationLanguageLevel = virtualFile.getUserData(CheckIOUtils.CHECKIO_LANGUAGE_LEVEL_KEY);
          if (publicationLanguageLevel != null) {
            final Sdk sdk = PythonSdkType.findPythonSdk(module);
            final LanguageLevel projectLanguageLevel = PythonSdkType.getLanguageLevelForSdk(sdk);
            if (projectLanguageLevel.isPy3K() != publicationLanguageLevel.isPy3K()) {
              String level = publicationLanguageLevel.isPy3K() ? "3" : "2.7";
              registerProblem(node, CheckIOBundle.message("inspection.language.level.mismatch.text", projectLanguageLevel, level),
                        new ConfigureInterpreterFix());
            }
          }
        }
      }
    }
  }

  private static class ConfigureInterpreterFix implements LocalQuickFix {
    @NotNull
    @Override
    public String getName() {
      return "Configure python interpreter";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return "Configure python interpreter";
    }

    @Override
    public void applyFix(@NotNull final Project project, @NotNull ProblemDescriptor descriptor) {
      ApplicationManager.getApplication()
        .invokeLater(() -> ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project Interpreter"));
    }
  }
}
