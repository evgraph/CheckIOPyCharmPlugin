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
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
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
  private final static String MISSION_PARAMETER_NAME = "mission";
  private final static String PUBLICATION_PARAMETER_NAME = "publications";
  private static final String MISSION_URL = "http://www.checkio.org/mission/";
  private static final String PUBLICATION_SUFFIX = "/publications/";
  private final static String ADD_PARAMETER_NAME = "add";
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

    ApplicationManager.getApplication().invokeLater(() -> {
      BalloonBuilder balloonBuilder =
        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, null, color, null);
      final Balloon balloon = balloonBuilder.createBalloon();
      StudyUtils.showCheckPopUp(project, balloon);
    });
  }

  public static String getTaskFileNameFromTask(@NotNull final Task task) {
    return task.getName() + ".py";
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

  public static File createPublicationFile(@NotNull final Project project, @NotNull final String taskName, @NotNull CheckIOPublication publication) {
    final File directory = getPublicationsDirectory(project, taskName);
      final String publicationNameWithoutExtension = publication.getPublicationFileNameWithExtension();
      final File file = new File(directory, publicationNameWithoutExtension);
      FileUtil.createIfDoesntExist(file);
      try {
        FileUtil.writeToFile(file, publication.getCode());
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    return file;
  }

  private static File getPublicationsDirectory(@NotNull final Project project, @NotNull final String taskName) {
    final String publicationDirectory = project.getBasePath() + PUBLICATION_FOLDER_NAME + taskName;
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

  public static String getSeePublicationsOnWebLink(@NotNull final String taskName) {
    return MISSION_URL + taskName + PUBLICATION_SUFFIX;
  }

  public static String getInterpreter(@NotNull final Task task, @NotNull final Project project) {
    final Sdk sdk = StudyUtils.findSdk(task, project);
    String runner = "";
    if (sdk != null) {
      String sdkName = sdk.getName();
      if (sdkName.substring(7, sdkName.length()).startsWith("2")) {
        runner = "python-27";
      }
      else {
        runner = "python-3";
      }
    }

    return runner;
  }


  public static String getAddPublicationLink(@NotNull final Project project, @NotNull final Task task) {
    String publicationLink = "";

    final CheckIOTaskManager checkIOTaskManager = CheckIOTaskManager.getInstance(project);
    final String token = checkIOTaskManager.getAccessToken();
    try {
      final URI uri = new URIBuilder(CheckIOPublication.PUBLICATION_URL)
        .addParameter("token", token)
        .addParameter("interpreter", getInterpreter(task, project))
        .addParameter("next", "")
        .build();
      publicationLink = uri.toString() + createAddPublicationLinkParameter(task.getName());
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return publicationLink;
  }

  private static String createAddPublicationLinkParameter(@NotNull final String taskName) {
    return String.join("/", new String[]{"", MISSION_PARAMETER_NAME, taskName, PUBLICATION_PARAMETER_NAME, ADD_PARAMETER_NAME, ""});
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
    catch (IOException | RuntimeException e) {
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
