import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.python.newProject.PythonBaseProjectGenerator;
import icons.PythonIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class CheckIOProjectGenerator extends PythonBaseProjectGenerator implements DirectoryProjectGenerator {

  CheckIONewProjectPanel mySettingsPanel;

  @Nls
  @NotNull
  @Override
  public String getName() {
    return "Create CheckIO Project";
  }

  @Nullable
  @Override
  public Object showGenerationSettings(VirtualFile baseDir) throws ProcessCanceledException {
    return null;
  }



  @Nullable
  @Override
  public Icon getLogo() {
    return PythonIcons.Python.Python_logo;
  }


  @NotNull
  @Override
  public ValidationResult validate(@NotNull String baseDirPath) {
    String message = "";
    //message = StringUtil.isEmptyOrSpaces(mySettingsPanel.getIslandName()) ? "Select an island" : "";

    return message.isEmpty() ? ValidationResult.OK : new ValidationResult(message);
  }

  @Nullable
  @Override
  public JPanel extendBasePanel() throws ProcessCanceledException {
    mySettingsPanel = new CheckIONewProjectPanel();
    mySettingsPanel.registerValidators(new FacetValidatorsManager() {
      public void registerValidator(FacetEditorValidator validator, JComponent... componentsToWatch) {
        throw new UnsupportedOperationException();
      }

      public void validate() {
        fireStateChanged();
      }
    });
    return mySettingsPanel.getMainPanel();
  }
}
