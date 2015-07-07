package main;

import com.jetbrains.edu.courseFormat.Task;
import org.junit.Assert;
import org.junit.Test;


public class CheckIOUtilsTest extends Assert {

  @Test
  public void testAddAnswerPlaceHolderIfDoesntExist() throws Exception {
    Task task = new Task();
    CheckIOUtils.addAnswerPlaceholderIfDoesntExist(task);
    assertEquals(new Task(), task);
  }
}