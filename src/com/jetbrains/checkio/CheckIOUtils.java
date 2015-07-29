package com.jetbrains.checkio;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowEP;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.courseFormat.*;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.python.psi.LanguageLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CheckIOUtils {
  public static final String USER_INFO_TOOL_WINDOW_ID = "User Info";
  public static final Key<LanguageLevel> CHECKIO_LANGUAGE_LEVEL_KEY = new Key<>("CheckIOLanguageLevel");
  private static final Logger LOG = Logger.getInstance(CheckIOUtils.class.getName());
  public static final int width = 450;
  public static final int height = 1000;
  public static final String PUBLICATION_FOLDER_NAME = "/.publications/";


  private CheckIOUtils() {
  }

  @Nullable
  public static CheckIOTaskToolWindowFactory getCheckIOToolWindowFactory() {
    final ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);
    for (ToolWindowEP toolWindowEP : toolWindowEPs) {
      if (toolWindowEP.id.equals(CheckIOToolWindow.ID)) {
        return (CheckIOTaskToolWindowFactory)toolWindowEP.getToolWindowFactory();
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

  public static void addAnswerPlaceholderIfDoesntExist(@NotNull final Task task) {
    final String taskFileName = getTaskFileNameFromTask(task);
    final TaskFile taskFile;
    if ((taskFile = task.getTaskFile(taskFileName)) != null) {
      if (taskFile.getAnswerPlaceholders().isEmpty()) {
        AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
        answerPlaceholder.setTaskText(taskFileName);
        answerPlaceholder.setIndex(0);
        taskFile.addAnswerPlaceholder(answerPlaceholder);
      }
    }
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
        FileUtil.writeToFile(file, publication.myText);
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
      final VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, false);
      if (virtualFile != null) {
        virtualFile.putUserDataIfAbsent(CHECKIO_LANGUAGE_LEVEL_KEY, publication.getLanguageLevel());
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


  public static File getPublicationsDirectory(@NotNull final Project project, @NotNull final Task task) {
    final String publicationDirectory = project.getBasePath() + PUBLICATION_FOLDER_NAME + task.getName();
    final File publicationDir = new File(publicationDirectory);
    if (!publicationDir.mkdirs()) {
      LOG.info(publicationDir + "already exists");
    }
    return publicationDir;
  }

  public static String getPublicationLink(@NotNull final CheckIOPublication publication) {
    return "";
  }

  public static String getUserProfileLink(@NotNull final CheckIOUser user) {
    return "http://www.checkio.org/user/" + user.getUsername();
  }

  public static boolean isPublicationFile(@NotNull final VirtualFile file) {
    return file.getUserData(CHECKIO_LANGUAGE_LEVEL_KEY) != null;
  }
}
