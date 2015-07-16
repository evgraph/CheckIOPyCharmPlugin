package com.jetbrains.checkio;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowEP;
import com.jetbrains.checkio.editor.CheckIOTextEditor;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.edu.courseFormat.*;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CheckIOUtils {
  public static final String TOOL_WINDOW_ID = "Task Info";
  private static final Logger LOG = Logger.getInstance(CheckIOUtils.class.getName());

  private CheckIOUtils() {
  }

  @Nullable
  public static CheckIOTaskToolWindowFactory getCheckIOToolWindowFactory(@NotNull final ToolWindowEP[] toolWindowEPs) {
    for (ToolWindowEP toolWindowEP : toolWindowEPs) {
      if (toolWindowEP.id.equals(TOOL_WINDOW_ID)) {
        return (CheckIOTaskToolWindowFactory)toolWindowEP.getToolWindowFactory();
      }
    }
    return null;
  }

  @Nullable
  public static Document getDocumentFromSelectedEditor(@NotNull final Project project) {
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

  private static void showInfoPopUp(@NotNull final Project project, @NotNull final Balloon balloon, @NotNull final JButton button) {
    final CheckIOTextEditor studyEditor = CheckIOTextEditor.getSelectedEditor(project);

    assert studyEditor != null;
    balloon.showInCenterOf(button);
    Disposer.register(project, balloon);
  }

  public static void showOperationResultPopUp(final String text,
                                              Color color,
                                              @NotNull final Project project,
                                              @NotNull final JButton button) {
    BalloonBuilder balloonBuilder =
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, null, color, null);
    final Balloon balloon = balloonBuilder.createBalloon();
    final CheckIOTextEditor textEditor = CheckIOTextEditor.getSelectedEditor(project);
    if (textEditor != null) {
      showInfoPopUp(project, balloon, button);
    }
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
      final File myCoursesDir = new File(PathManager.getConfigPath(), "courses");
      flushLesson(newLesson, myCoursesDir + oldCourse.getName());
      index++;
    }
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
  }

  private static void flushLesson(Lesson lesson, String courseDirectory) {
    File lessonDirectory = new File(courseDirectory, "lesson" + String.valueOf(lesson.getIndex()));
    FileUtil.createDirectory(lessonDirectory);
    int taskIndex = 1;
    for(Iterator i$1 = lesson.taskList.iterator(); i$1.hasNext(); ++taskIndex) {
      Task task = (Task)i$1.next();
      File taskDirectory = new File(lessonDirectory, "task" + String.valueOf(taskIndex));
      FileUtil.createDirectory(taskDirectory);
      Iterator testsText = task.taskFiles.entrySet().iterator();

      while(testsText.hasNext()) {
        Map.Entry taskText = (Map.Entry)testsText.next();
        String e = (String)taskText.getKey();
        TaskFile testsFile = (TaskFile)taskText.getValue();
        File e1 = new File(taskDirectory, e);
        FileUtil.createIfDoesntExist(e1);

        try {
          FileUtil.writeToFile(e1, testsFile.text);
        } catch (IOException var17) {
          LOG.error("ERROR copying file " + e);
        }
      }

      Map var20 = task.getTestsText();
      Iterator var21 = var20.entrySet().iterator();

      while(var21.hasNext()) {
        Map.Entry var23 = (Map.Entry)var21.next();
        File var24 = new File(taskDirectory, (String)var23.getKey());
        FileUtil.createIfDoesntExist(var24);

        try {
          FileUtil.writeToFile(var24, (String)var23.getValue());
        } catch (IOException var19) {
          LOG.error("ERROR copying tests file");
        }
      }

      File var22 = new File(taskDirectory, "task.html");
      FileUtil.createIfDoesntExist(var22);

      try {
        FileUtil.writeToFile(var22, task.getText());
      } catch (IOException var18) {
        LOG.error("ERROR copying tests file");
      }
    }

  }
}
