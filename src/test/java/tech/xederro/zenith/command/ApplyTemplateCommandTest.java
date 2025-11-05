/**
 * @license
 * Copyright (C) 2025 Dawid Jabłoński
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.xederro.zenith.command;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(Parameterized.class)
public class ApplyTemplateCommandTest {
  @Mock private FileRepoHelper fileRepoHelper;

  private ApplyTemplateCommand command;
  private ByteArrayOutputStream stdoutStream;
  private ByteArrayOutputStream stderrStream;

  @Parameter public String testName;
  @Parameter(1) public String projectName;
  @Parameter(2) public String targets;
  @Parameter(3) public String jsonInput;
  @Parameter(4) public Exception mockException;
  @Parameter(5) public int expectedCreateCommitCalls;
  @Parameter(6) public List<String> expectedTargetsList;
  @Parameter(7) public String expectedStdoutContains;
  @Parameter(8) public String expectedStderrContains;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    Gson gson = new Gson();

    Map<String, String> simpleJson = new HashMap<>();
    simpleJson.put("key", "value");

    Map<String, Object> complexJson = new HashMap<>();
    complexJson.put("name", "test");
    complexJson.put("version", 1.0);
    complexJson.put("enabled", true);
    complexJson.put("tags", Arrays.asList("tag1", "tag2"));

    String nestedJson =
        "{\"config\":{\"database\":{\"host\":\"localhost\",\"port\":5432},\"enabled\":true}}";

    return Arrays.asList(
      new Object[][] {
        {
          "Single target with JSON data",
          "test-project",
          "TEMPLATE@master:master",
          gson.toJson(simpleJson),
          null,
          1,
          List.of("TEMPLATE@master:master"),
          "Applied template to test-project",
          null
        },{
          "Multiple targets with JSON data",
          "test-project",
          "TEMPLATE@master:master,TEMPLATE@config:refs/meta/config",
          gson.toJson(simpleJson),
          null,
          2,
          Arrays.asList("TEMPLATE@master:master", "TEMPLATE@config:refs/meta/config"),
          "Applied template to test-project",
          null
        },{
          "Targets with surrounding whitespace",
          "test-project",
          "TEMPLATE@master:master, TEMPLATE@config:refs/meta/config , TEMPLATE@other:refs/meta/config",
          null,
          null,
          3,
          Arrays.asList(
            "TEMPLATE@master:master",
            "TEMPLATE@config:refs/meta/config",
            "TEMPLATE@other:refs/meta/config"),
          "Applied template to test-project",
          null
        },{
          "No targets specified (null)",
          "test-project",
          null,
          null,
          null,
          0,
          null,
          "Applied template to test-project",
          null
        },{
          "Complex JSON object with nested structures",
          "test-project",
          "refs/heads/main:config.json",
          gson.toJson(complexJson),
          null,
          1,
          List.of("refs/heads/main:config.json"),
          "Applied template to test-project",
          null
        },{
          "Exception thrown by FileRepoHelper",
          "test-project",
          "TEMPLATE@other:refs/meta/config",
          null,
          new RuntimeException("Repository not found"),
          1,
          null,
          null,
          "error: Repository not found"
        },{
          "Empty string as targets input",
          "test-project",
          "",
          null,
          null,
          1,
          List.of(""),
          "Applied template to test-project",
          null
        },{
          "Special characters in project name",
          "test/project-name_123",
          "TEMPLATE@other:refs/meta/config",
          null,
          null,
          1,
          List.of("TEMPLATE@other:refs/meta/config"),
          "Applied template to test/project-name_123",
          null
        },{
          "JSON array input",
          "test-project",
          "TEMPLATE@other:refs/meta/config",
          "[\"item1\",\"item2\",\"item3\"]",
          null,
          1,
          List.of("TEMPLATE@other:refs/meta/config"),
          "Applied template to test-project",
          null
        },{
          "Deeply nested JSON structure",
          "test-project",
          "refs/heads/main:config.yaml",
          nestedJson,
          null,
          1,
          List.of("refs/heads/main:config.yaml"),
          "Applied template to test-project",
          null
        }
      });
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    Gson gson = new Gson();
    command = new ApplyTemplateCommand(fileRepoHelper, gson);

    stdoutStream = new ByteArrayOutputStream();
    stderrStream = new ByteArrayOutputStream();
    PrintWriter stdout = new PrintWriter(stdoutStream, true);
    PrintWriter stderr = new PrintWriter(stderrStream, true);

    setProtectedField(command, "stdout", stdout);
    setProtectedField(command, "stderr", stderr);
  }

  @Test
  public void testApplyTemplateCommand() throws Exception {
    // Given
    command.projectName = projectName;
    if (targets != null) {
      command.setTargets(targets);
    }
    if (jsonInput != null) {
      command.setJson(jsonInput);
    }
    if (mockException != null) {
      doThrow(mockException).when(fileRepoHelper).createCommit(any(), any(), any(), anyBoolean());
    }

    // When
    command.run();

    // Then
    verify(fileRepoHelper, times(expectedCreateCommitCalls)).createCommit(any(), any(), any(), anyBoolean());
    if (expectedTargetsList != null) {
      for (String expectedTarget : expectedTargetsList) {
        verify(fileRepoHelper, atLeastOnce())
            .createCommit(eq(projectName), eq(expectedTarget), any(), anyBoolean());
      }
    }
    if (expectedStdoutContains != null) {
      assertTrue(
          "Expected stdout to contain: " + expectedStdoutContains,
          stdoutStream.toString().contains(expectedStdoutContains));
    }
    if (expectedStderrContains != null) {
      assertTrue(
          "Expected stderr to contain: " + expectedStderrContains,
          stderrStream.toString().contains(expectedStderrContains));
    }
  }

  private void setProtectedField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
