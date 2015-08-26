package com.jetbrains.checkio;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.learning.editor.StudyEditor;
import com.jetbrains.python.psi.LanguageLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class CheckIOUtils {
  public static final Key<LanguageLevel> CHECKIO_LANGUAGE_LEVEL_KEY = new Key<>("CheckIOLanguageLevel");
  public static final int WIDTH = 450;
  public static final int HEIGHT = 1000;
  public static final int MAX_WIDTH = 2000;
  public static final int MAX_HEIGHT = 2000;
  public static final String PUBLICATION_FOLDER_NAME = "/.publications/";
  public static final String COURSE_TYPE = "CHECK_IO";
  private static final Logger LOG = Logger.getInstance(CheckIOUtils.class.getName());

  private CheckIOUtils() {
  }

  @Nullable
  public static ToolWindowFactory getToolWindowFactoryById(@NotNull final String id) {
    final ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);
    for (ToolWindowEP toolWindowEP : toolWindowEPs) {
      if (toolWindowEP.id.equals(id)) {
        return toolWindowEP.getToolWindowFactory();
      }
    }
    return null;
  }

  @Nullable
  private static Document getDocumentFromSelectedEditor(@NotNull final Project project) {
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    assert fileEditorManager != null;
    Editor editor = fileEditorManager.getSelectedTextEditor();
    if (editor != null) {
      return editor.getDocument();
    }
    return null;
  }

  @Nullable
  public static Task getTaskFromSelectedEditor(@NotNull final Project project) {
    FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
    Document document = getDocumentFromSelectedEditor(project);
    if (document != null) {
      VirtualFile file = fileDocumentManager.getFile(document);
      assert file != null;
      StudyUtils.getTaskFile(project, file);
      TaskFile taskFile = StudyUtils.getTaskFile(project, file);
      if (taskFile != null) {
        return taskFile.getTask();
      }
    }
    return null;
  }

  public static void showOperationResultPopUp(final String text,
                                              Color color,
                                              @NotNull final Project project) {
    BalloonBuilder balloonBuilder =
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, null, color, null);
    final Balloon balloon = balloonBuilder.createBalloon();
    StudyUtils.showCheckPopUp(project, balloon);
  }

  public static String getTaskFileNameFromTask(@NotNull final Task task) {
    return task.getName() + ".py";
  }

  public static String getTaskTextUrl(@NotNull final Project project, @Nullable final Task task) {
    if (task == null) {
      return "";
    }
    final VirtualFile virtualFile = task.getTaskDir(project);
    assert virtualFile != null;
    return "file://" + virtualFile.getCanonicalPath() + "/task.html";
  }

  public static void createNewLessonsDirsAndFlush(@NotNull final Course oldCourse,
                                                  @NotNull final Course newCourse,
                                                  @NotNull final Project project) {
    final VirtualFile baseDir = project.getBaseDir();
    final List<Lesson> oldLessons = oldCourse.getLessons();
    final List<Lesson> newLessons = newCourse.getLessons();
    int index = 1;
    for (Lesson newLesson : newLessons) {
      boolean isNew = true;
      for (Lesson oldLesson : oldLessons) {
        if (newLesson.getName().equals(oldLesson.getName())) {
          isNew = false;
          break;
        }
      }
      if (isNew) {
        oldCourse.addLesson(newLesson);
        newLesson.setIndex(index);
        ApplicationManager.getApplication().runWriteAction(() -> {
          try {
            StudyGenerator.createLesson(newLesson, baseDir, new File(oldCourse.getCourseDirectory()), project);
          }
          catch (IOException e) {
            LOG.warn(e.getMessage());
          }
        });
      }
      final File myCoursesDir = new File(PathManager.getConfigPath(), "courses" + oldCourse.getName());
      StudyProjectGenerator.flushLesson(myCoursesDir, newLesson);
      index++;
    }
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
  }

  public static void createPublicationsFiles(@NotNull Project project,
                                             @NotNull final Task task,
                                             @NotNull final CheckIOPublication[] publications) {
    final File directory = getPublicationsDirectory(project, task);
    for (CheckIOPublication publication : publications) {
      final String publicationNameWithoutExtension = publication.getPublicationFileNameWithExtension();
      final File file = new File(directory, publicationNameWithoutExtension);
      FileUtil.createIfDoesntExist(file);
      try {
        FileUtil.writeToFile(file, publication.getText());
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }

  }

  public static VirtualFile getPublicationFile(@NotNull final Project project,
                                               @NotNull final String publicationNameWithExtension,
                                               @NotNull Task task) {
    final VirtualFile baseDirectory = project.getBaseDir();
    final VirtualFile publicationsDirectory = baseDirectory.findChild(".publications");
    if (publicationsDirectory != null) {
      final VirtualFile publicationDirectoryForTask = publicationsDirectory.findChild(task.getName());
      if (publicationDirectoryForTask != null) {
        return publicationDirectoryForTask.findChild(publicationNameWithExtension);
      }
    }
    return null;
  }


  private static File getPublicationsDirectory(@NotNull final Project project, @NotNull final Task task) {
    final String publicationDirectory = project.getBasePath() + PUBLICATION_FOLDER_NAME + task.getName();
    final File publicationDir = new File(publicationDirectory);
    if (!publicationDir.mkdirs()) {
      LOG.info(publicationDir + "already exists");
    }
    return publicationDir;
  }

  public static void updateTaskToolWindow(@NotNull final Project project) {
    ToolWindowManager.getInstance(project).getToolWindow(CheckIOToolWindow.ID).
      getContentManager().removeAllContents(false);
    final CheckIOTaskToolWindowFactory factory = (CheckIOTaskToolWindowFactory) getToolWindowFactoryById(CheckIOToolWindow.ID);
    if (factory != null) {
      factory.createToolWindowContent(project, ToolWindowManager.getInstance(project).
        getToolWindow(CheckIOToolWindow.ID));
    }
  }

  public static void selectCurrentTask(@NotNull final Project project) {
    final StudyEditor selectedEditor = StudyUtils.getSelectedStudyEditor(project);
    FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (selectedEditor != null) {
      final VirtualFile virtualFile = FileEditorManagerEx.getInstanceEx(project).getFile(selectedEditor);
      if (virtualFile != null) {
        final PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
        VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
        ProjectView.getInstance(project).select(file, virtualFile, true);
      }
    }
  }

  //TODO: update (api needed)
  public static String getPublicationLink(@NotNull final CheckIOPublication publication) {
    return "";
  }

  //TODO: update (api needed)
  public static String getForumLink(@NotNull Task task, @NotNull final Project project) {
    final Sdk sdk= StudyUtils.findSdk(task, project);
    int taskId = CheckIOTaskManager.getInstance(project).getTaskId(task);
    String link = "";
    if (sdk!= null) {
      final String version = sdk.getVersionString();
      link = "http://www.checkio.org/forum/add/?source=hints&task_id=" +
             taskId +
             "t&interpreter=python-" +
             version +
             " &q=tag%3Afor_advisers,hint.bryukh";
    }
    return  link;
  }

  //TODO: update (api needed)
  public static String getUserProfileLink(@NotNull final CheckIOUser user) {
    return "http://www.checkio.org/user/" + user.getUsername();
  }

  public static boolean isPublicationFile(@NotNull final VirtualFile file) {
    return file.getUserData(CHECKIO_LANGUAGE_LEVEL_KEY) != null;
  }

  public static boolean checkConnection() {
    boolean result = false;
    try {
      URL urlToPing = new URL ("http://www.checkio.org");
      HttpURLConnection connection = (HttpURLConnection)urlToPing.openConnection();

      connection.setRequestMethod("GET");
      connection.connect();

      int code = connection.getResponseCode();
      if (code == 200) {
        result = true;
      }
    }
    catch (IOException e) {
      result = false;
    }

    return result;
  }

  public static void makeNoInternetConnectionNotifier(@NotNull Project project) {
    final Notification notification = new Notification("No.connection", "Internet connection problem", "CheckIO is unavailable. Please, check internet connection",
                                                       NotificationType.ERROR);
    notification.notify(project);
  }
}
