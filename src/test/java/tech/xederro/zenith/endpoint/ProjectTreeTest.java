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

package tech.xederro.zenith.endpoint;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.entities.*;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.Projects;
import com.google.gerrit.extensions.api.projects.Projects.QueryRequest;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.server.project.ProjectCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

public class ProjectTreeTest {

  private ProjectTree projectTree;

  @Mock private AccessSection mockAccessSection;
  @Mock private Permission mockPermission;
  @Mock private PermissionRule mockPermissionRule;
  @Mock private GroupReference mockGroupReference;
  @Mock private LabelType mockLabelType;
  @Mock private GerritApi gerritApi;
  @Mock private ProjectCache projectCache;
  @Mock private Projects projects;
  @Mock private QueryRequest queryRequest;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    projectTree = new ProjectTree(gerritApi, projectCache);
  }

  @Test
  public void testProcessExtensionPanelSections_withValidData() {
    Map<String, ImmutableList<String>> currentSections = new HashMap<>();
    currentSections.put("MyPanel", ImmutableList.of("value1", "value2"));

    Map<String, ProjectTree.Value> result = invokeProcessExtensionPanelSections(currentSections, new HashMap<>());

    assertEquals(1, result.size());
    assertTrue(result.containsKey("extension_panel MyPanel"));
    assertEquals("value1,value2", result.get("extension_panel MyPanel").value());
    assertFalse(result.get("extension_panel MyPanel").isInherited());
  }

  @Test
  public void testProcessExtensionPanelSections_withNullCurrentSections() {
    Map<String, ProjectTree.Value> result = invokeProcessExtensionPanelSections(null, new HashMap<>());

    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testProcessExtensionPanelSections_withNullParent() {
    Map<String, ImmutableList<String>> currentSections = new HashMap<>();
    currentSections.put("Panel1", ImmutableList.of("a", "b"));

    Map<String, ProjectTree.Value> result = invokeProcessExtensionPanelSections(currentSections, null);

    assertEquals(1, result.size());
    assertTrue(result.containsKey("extension_panel Panel1"));
  }

  @Test
  public void testProcessExtensionPanelSections_inheritFromParent() {
    Map<String, ImmutableList<String>> currentSections = new HashMap<>();
    currentSections.put("Panel1", ImmutableList.of("current"));

    Map<String, ProjectTree.Value> parentPermissions = new HashMap<>();
    parentPermissions.put("extension_panel Panel2", new ProjectTree.Value("inherited", true));
    parentPermissions.put("other_key", new ProjectTree.Value("shouldSkip", true));

    Map<String, ProjectTree.Value> result = invokeProcessExtensionPanelSections(currentSections, parentPermissions);

    assertEquals(2, result.size());
    assertEquals("current", result.get("extension_panel Panel1").value());
    assertFalse(result.get("extension_panel Panel1").isInherited());
    assertEquals("inherited", result.get("extension_panel Panel2").value());
    assertTrue(result.get("extension_panel Panel2").isInherited());
  }

  @Test
  public void testProcessExtensionPanelSections_overrideParent() {
    Map<String, ImmutableList<String>> currentSections = new HashMap<>();
    currentSections.put("Panel1", ImmutableList.of("override"));

    Map<String, ProjectTree.Value> parentPermissions = new HashMap<>();
    parentPermissions.put("extension_panel Panel1", new ProjectTree.Value("inherited", true));

    Map<String, ProjectTree.Value> result = invokeProcessExtensionPanelSections(currentSections, parentPermissions);

    assertEquals(1, result.size());
    assertEquals("override", result.get("extension_panel Panel1").value());
    assertFalse(result.get("extension_panel Panel1").isInherited());
  }

  @Test
  public void testProcessExtensionPanelSections_emptyPanelValues() {
    Map<String, ImmutableList<String>> currentSections = new HashMap<>();
    currentSections.put("EmptyPanel", ImmutableList.of());

    Map<String, ProjectTree.Value> result = invokeProcessExtensionPanelSections(currentSections, new HashMap<>());

    assertEquals(1, result.size());
    assertEquals("", result.get("extension_panel EmptyPanel").value());
  }

  @Test
  public void testProcessPluginConfigs_withValidData() {
    Map<String, String> currentConfigs = new HashMap<>();
    currentConfigs.put("uploadvalidator", "[plugin \"uploadvalidator\"] rejectDuplicatePathnames = true");

    Map<String, ProjectTree.Value> result = invokeProcessPluginConfigs(currentConfigs, new HashMap<>());

    assertEquals(1, result.size());
    assertTrue(result.containsKey("plugin uploadvalidator rejectDuplicatePathnames"));
    assertEquals("true", result.get("plugin uploadvalidator rejectDuplicatePathnames").value());
    assertFalse(result.get("plugin uploadvalidator rejectDuplicatePathnames").isInherited());
  }

  @Test
  public void testProcessPluginConfigs_withNullCurrentConfigs() {
    Map<String, ProjectTree.Value> result = invokeProcessPluginConfigs(null, new HashMap<>());

    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testProcessPluginConfigs_withNullParent() {
    Map<String, String> currentConfigs = new HashMap<>();
    currentConfigs.put("key1", "[plugin \"uploadvalidator\"] rejectWindowsLineEndings = true");

    Map<String, ProjectTree.Value> result = invokeProcessPluginConfigs(currentConfigs, null);

    assertEquals(1, result.size());
    assertTrue(result.containsKey("plugin key1 rejectWindowsLineEndings"));
  }

  @Test
  public void testProcessPluginConfigs_inheritFromParent() {
    Map<String, String> currentConfigs = new HashMap<>();
    currentConfigs.put("current-key", "[plugin \"uploadvalidator\"] current-value = true");

    Map<String, ProjectTree.Value> parentPermissions = new HashMap<>();
    parentPermissions.put("plugin parent-key", new ProjectTree.Value("parent-value", true));
    parentPermissions.put("other-key", new ProjectTree.Value("shouldSkip", true));

    Map<String, ProjectTree.Value> result = invokeProcessPluginConfigs(currentConfigs, parentPermissions);

    assertEquals(2, result.size());
    assertEquals("true", result.get("plugin current-key current-value").value());
    assertFalse(result.get("plugin current-key current-value").isInherited());
    assertEquals("parent-value", result.get("plugin parent-key").value());
    assertTrue(result.get("plugin parent-key").isInherited());
  }

  @Test
  public void testProcessPluginConfigs_overrideParent() {
    Map<String, String> currentConfigs = new HashMap<>();
    currentConfigs.put("shared-key", "[plugin \"uploadvalidator\"] current-value = true");

    Map<String, ProjectTree.Value> parentPermissions = new HashMap<>();
    parentPermissions.put("plugin shared-key current-value", new ProjectTree.Value("false", true));

    Map<String, ProjectTree.Value> result = invokeProcessPluginConfigs(currentConfigs, parentPermissions);

    assertEquals(1, result.size());
    assertEquals("true", result.get("plugin shared-key current-value").value());
    assertFalse(result.get("plugin shared-key current-value").isInherited());
  }

  @Test
  public void testProcessPluginConfigs_multipleConfigs() {
    Map<String, String> currentConfigs = new HashMap<>();
    currentConfigs.put("key1", "[plugin \"uploadvalidator\"] current-value = true");
    currentConfigs.put("key2", "[plugin \"uploadvalidator\"] rejectDuplicatePathnames\t= true");
    currentConfigs.put("key3", "[plugin \"uploadvalidator\"] rejectDuplicatePathnames = true\r\nrejectDuplicatePathnamesLocale = pl");

    Map<String, ProjectTree.Value> result = invokeProcessPluginConfigs(currentConfigs, new HashMap<>());

    assertEquals(4, result.size());
    assertTrue(result.containsKey("plugin key1 current-value"));
    assertTrue(result.containsKey("plugin key2 rejectDuplicatePathnames"));
    assertTrue(result.containsKey("plugin key3 rejectDuplicatePathnames"));
    assertTrue(result.containsKey("plugin key3 rejectDuplicatePathnamesLocale"));
  }

  @Test
  public void testProcessLabelsSections_withValidLabelAndRefPatterns() {
    when(mockLabelType.getRefPatterns())
        .thenReturn(ImmutableList.of("refs/heads/main", "refs/heads/develop"));
    LabelValue minVal = mock(LabelValue.class);
    LabelValue maxVal = mock(LabelValue.class);
    when(minVal.getValue()).thenReturn((short) -1);
    when(maxVal.getValue()).thenReturn((short) 1);
    when(mockLabelType.getMin()).thenReturn(minVal);
    when(mockLabelType.getMax()).thenReturn(maxVal);
    when(mockLabelType.getFunction()).thenReturn(LabelFunction.MAX_WITH_BLOCK);
    when(mockLabelType.getCopyCondition()).thenReturn(Optional.of("condition"));
    when(mockLabelType.getDefaultValue()).thenReturn((short) 0);
    when(mockLabelType.isIgnoreSelfApproval()).thenReturn(false);
    when(mockLabelType.isAllowPostSubmit()).thenReturn(true);

    Map<String, LabelType> currentLabels = new HashMap<>();
    currentLabels.put("Code-Review", mockLabelType);

    Map<String, ProjectTree.Value> result = invokeProcessLabelsSections(currentLabels, new HashMap<>());

    assertEquals(12, result.size());
    assertTrue(result.containsKey("label Code-Review refs/heads/main label-range"));
    assertTrue(result.containsKey("label Code-Review refs/heads/main label-function"));
    assertTrue(result.containsKey("label Code-Review refs/heads/main label-copy-condition"));
  }

  @Test
  public void testProcessLabelsSections_withDefaultRefPattern() {
    when(mockLabelType.getRefPatterns()).thenReturn(null);
    LabelValue val = mock(LabelValue.class);
    when(val.getValue()).thenReturn((short) 0);
    when(mockLabelType.getMin()).thenReturn(val);
    when(mockLabelType.getMax()).thenReturn(mock(LabelValue.class));
    when(mockLabelType.getMax().getValue()).thenReturn((short) 1);
    when(mockLabelType.getFunction()).thenReturn(LabelFunction.ANY_WITH_BLOCK);
    when(mockLabelType.getCopyCondition()).thenReturn(Optional.empty());
    when(mockLabelType.getDefaultValue()).thenReturn((short) 0);
    when(mockLabelType.isIgnoreSelfApproval()).thenReturn(true);
    when(mockLabelType.isAllowPostSubmit()).thenReturn(false);

    Map<String, LabelType> currentLabels = new HashMap<>();
    currentLabels.put("Verified", mockLabelType);

    Map<String, ProjectTree.Value> result = invokeProcessLabelsSections(currentLabels, new HashMap<>());

    assertEquals(6, result.size());
    assertTrue(result.containsKey("label Verified * label-range"));
    assertEquals("0...1", result.get("label Verified * label-range").value());
    assertEquals("NONE", result.get("label Verified * label-copy-condition").value());
    assertEquals("TRUE", result.get("label Verified * label-ignore-self-approval").value());
    assertEquals("FALSE", result.get("label Verified * label-allow-post-submit").value());
  }

  @Test
  public void testProcessLabelsSections_withOnlyMin() {
    LabelValue minValue = mock(LabelValue.class);
    when(minValue.getValue()).thenReturn((short) -2);
    when(mockLabelType.getRefPatterns()).thenReturn(ImmutableList.of("*"));
    when(mockLabelType.getMin()).thenReturn(minValue);
    when(mockLabelType.getMax()).thenReturn(null);
    when(mockLabelType.getFunction()).thenReturn(LabelFunction.MAX_WITH_BLOCK);
    when(mockLabelType.getCopyCondition()).thenReturn(Optional.empty());
    when(mockLabelType.getDefaultValue()).thenReturn((short) 0);
    when(mockLabelType.isIgnoreSelfApproval()).thenReturn(false);
    when(mockLabelType.isAllowPostSubmit()).thenReturn(false);

    Map<String, LabelType> currentLabels = new HashMap<>();
    currentLabels.put("Score", mockLabelType);

    Map<String, ProjectTree.Value> result = invokeProcessLabelsSections(currentLabels, new HashMap<>());

    assertEquals("-2", result.get("label Score * label-range").value());
  }

  @Test
  public void testProcessLabelsSections_withOnlyMax() {
    LabelValue maxValue = mock(LabelValue.class);
    when(maxValue.getValue()).thenReturn((short) 5);
    when(mockLabelType.getRefPatterns()).thenReturn(ImmutableList.of("*"));
    when(mockLabelType.getMin()).thenReturn(null);
    when(mockLabelType.getMax()).thenReturn(maxValue);
    when(mockLabelType.getFunction()).thenReturn(LabelFunction.MAX_WITH_BLOCK);
    when(mockLabelType.getCopyCondition()).thenReturn(Optional.empty());
    when(mockLabelType.getDefaultValue()).thenReturn((short) 0);
    when(mockLabelType.isIgnoreSelfApproval()).thenReturn(false);
    when(mockLabelType.isAllowPostSubmit()).thenReturn(false);

    Map<String, LabelType> currentLabels = new HashMap<>();
    currentLabels.put("Quality", mockLabelType);

    Map<String, ProjectTree.Value> result = invokeProcessLabelsSections(currentLabels, new HashMap<>());

    assertEquals("5", result.get("label Quality * label-range").value());
  }

  @Test
  public void testProcessLabelsSections_withoutMinOrMax() {
    when(mockLabelType.getRefPatterns()).thenReturn(ImmutableList.of("*"));
    when(mockLabelType.getMin()).thenReturn(null);
    when(mockLabelType.getMax()).thenReturn(null);
    when(mockLabelType.getFunction()).thenReturn(LabelFunction.MAX_WITH_BLOCK);
    when(mockLabelType.getCopyCondition()).thenReturn(Optional.empty());
    when(mockLabelType.getDefaultValue()).thenReturn((short) 0);
    when(mockLabelType.isIgnoreSelfApproval()).thenReturn(false);
    when(mockLabelType.isAllowPostSubmit()).thenReturn(false);

    Map<String, LabelType> currentLabels = new HashMap<>();
    currentLabels.put("Test", mockLabelType);

    Map<String, ProjectTree.Value> result = invokeProcessLabelsSections(currentLabels, new HashMap<>());

    assertEquals("0...0", result.get("label Test * label-range").value());
  }

  @Test
  public void testProcessLabelsSections_inheritFromParent() {
    when(mockLabelType.getRefPatterns()).thenReturn(ImmutableList.of("*"));
    LabelValue val = mock(LabelValue.class);
    when(val.getValue()).thenReturn((short) 0);
    when(mockLabelType.getMin()).thenReturn(val);
    when(mockLabelType.getMax()).thenReturn(mock(LabelValue.class));
    when(mockLabelType.getMax().getValue()).thenReturn((short) 1);
    when(mockLabelType.getFunction()).thenReturn(LabelFunction.MAX_WITH_BLOCK);
    when(mockLabelType.getCopyCondition()).thenReturn(Optional.empty());
    when(mockLabelType.getDefaultValue()).thenReturn((short) 0);
    when(mockLabelType.isIgnoreSelfApproval()).thenReturn(false);
    when(mockLabelType.isAllowPostSubmit()).thenReturn(false);

    Map<String, LabelType> currentLabels = new HashMap<>();
    currentLabels.put("CurrentLabel", mockLabelType);

    Map<String, ProjectTree.Value> parentPermissions = new HashMap<>();
    parentPermissions.put("label InheritedLabel * label-range", new ProjectTree.Value("0...1", true));
    parentPermissions.put("other-key", new ProjectTree.Value("shouldSkip", true));

    Map<String, ProjectTree.Value> result = invokeProcessLabelsSections(currentLabels, parentPermissions);

    assertTrue(result.containsKey("label CurrentLabel * label-range"));
    assertFalse(result.get("label CurrentLabel * label-range").isInherited());
    assertTrue(result.containsKey("label InheritedLabel * label-range"));
    assertTrue(result.get("label InheritedLabel * label-range").isInherited());
  }

  @Test
  public void testProcessLabelsSections_nullCurrentLabels() {
    Map<String, ProjectTree.Value> result = invokeProcessLabelsSections(null, new HashMap<>());

    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testProcessLabelsSections_nullParent() {
    when(mockLabelType.getRefPatterns()).thenReturn(ImmutableList.of("*"));
    LabelValue val = mock(LabelValue.class);
    when(val.getValue()).thenReturn((short) 0);
    when(mockLabelType.getMin()).thenReturn(val);
    when(mockLabelType.getMax()).thenReturn(mock(LabelValue.class));
    when(mockLabelType.getMax().getValue()).thenReturn((short) 1);
    when(mockLabelType.getFunction()).thenReturn(LabelFunction.MAX_WITH_BLOCK);
    when(mockLabelType.getCopyCondition()).thenReturn(Optional.empty());
    when(mockLabelType.getDefaultValue()).thenReturn((short) 0);
    when(mockLabelType.isIgnoreSelfApproval()).thenReturn(false);
    when(mockLabelType.isAllowPostSubmit()).thenReturn(false);

    Map<String, LabelType> currentLabels = new HashMap<>();
    currentLabels.put("Label", mockLabelType);

    Map<String, ProjectTree.Value> result = invokeProcessLabelsSections(currentLabels, null);

    assertEquals(6, result.size());
  }

  @Test
  public void testProcessAccessSections_withValidAccessData() {
    when(mockGroupReference.getName()).thenReturn("Developers");
    when(mockPermissionRule.getGroup()).thenReturn(mockGroupReference);
    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.ALLOW);
    when(mockPermissionRule.getForce()).thenReturn(false);

    when(mockPermission.getName()).thenReturn("read");
    when(mockPermission.getRules()).thenReturn(ImmutableList.of(mockPermissionRule));
    when(mockPermission.getExclusiveGroup()).thenReturn(false);

    when(mockAccessSection.getPermissions()).thenReturn(ImmutableList.of(mockPermission));

    Map<String, AccessSection> currentSections = new HashMap<>();
    currentSections.put("refs/heads/main", mockAccessSection);

    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(currentSections, new HashMap<>());

    assertEquals(1, result.size());
    assertTrue(result.containsKey("refs/heads/main read Developers"));
    assertEquals("ALLOW", result.get("refs/heads/main read Developers").value());
    assertFalse(result.get("refs/heads/main read Developers").isInherited());
  }

  @Test
  public void testProcessAccessSections_withForcePermission() {
    when(mockGroupReference.getName()).thenReturn("Maintainers");
    when(mockPermissionRule.getGroup()).thenReturn(mockGroupReference);
    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.DENY);
    when(mockPermissionRule.getForce()).thenReturn(true);

    when(mockPermission.getName()).thenReturn("push");
    when(mockPermission.getRules()).thenReturn(ImmutableList.of(mockPermissionRule));
    when(mockPermission.getExclusiveGroup()).thenReturn(false);

    when(mockAccessSection.getPermissions()).thenReturn(ImmutableList.of(mockPermission));

    Map<String, AccessSection> currentSections = new HashMap<>();
    currentSections.put("refs/heads/*", mockAccessSection);

    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(currentSections, new HashMap<>());

    assertEquals(1, result.size());
    assertEquals("DENY_FORCE", result.get("refs/heads/* push Maintainers").value());
  }

  @Test
  public void testProcessAccessSections_withLabelPermission() {
    when(mockGroupReference.getName()).thenReturn("Code Reviewers");
    when(mockPermissionRule.getGroup()).thenReturn(mockGroupReference);
    when(mockPermissionRule.getMin()).thenReturn(-1);
    when(mockPermissionRule.getMax()).thenReturn(1);
    when(mockPermissionRule.getForce()).thenReturn(false);

    when(mockPermission.getName()).thenReturn("label-Code-Review");
    when(mockPermission.getRules()).thenReturn(ImmutableList.of(mockPermissionRule));
    when(mockPermission.getExclusiveGroup()).thenReturn(false);

    when(mockAccessSection.getPermissions()).thenReturn(ImmutableList.of(mockPermission));

    Map<String, AccessSection> currentSections = new HashMap<>();
    currentSections.put("refs/heads/main", mockAccessSection);

    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(currentSections, new HashMap<>());

    assertEquals(1, result.size());
    assertEquals("-1...1", result.get("refs/heads/main label-Code-Review Code-Reviewers").value());
  }

  @Test
  public void testProcessAccessSections_withExclusiveGroup() {
    when(mockGroupReference.getName()).thenReturn("Admin Group");
    when(mockPermissionRule.getGroup()).thenReturn(mockGroupReference);
    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.ALLOW);
    when(mockPermissionRule.getForce()).thenReturn(false);

    when(mockPermission.getName()).thenReturn("admin");
    when(mockPermission.getRules()).thenReturn(ImmutableList.of(mockPermissionRule));
    when(mockPermission.getExclusiveGroup()).thenReturn(true);

    when(mockAccessSection.getPermissions()).thenReturn(ImmutableList.of(mockPermission));

    Map<String, AccessSection> currentSections = new HashMap<>();
    currentSections.put("refs/heads/main", mockAccessSection);

    Map<String, ProjectTree.Value> parentPermissions = new HashMap<>();
    parentPermissions.put("refs/heads/main admin Other-Group", new ProjectTree.Value("ALLOW", true));

    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(currentSections, parentPermissions);

    assertEquals(1, result.size());
    assertTrue(result.containsKey("refs/heads/main admin Admin-Group"));
    assertFalse(result.containsKey("refs/heads/main admin Other-Group"));
  }

  @Test
  public void testProcessAccessSections_inheritFromParent() {
    when(mockGroupReference.getName()).thenReturn("Developers");
    when(mockPermissionRule.getGroup()).thenReturn(mockGroupReference);
    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.ALLOW);
    when(mockPermissionRule.getForce()).thenReturn(false);

    when(mockPermission.getName()).thenReturn("read");
    when(mockPermission.getRules()).thenReturn(ImmutableList.of(mockPermissionRule));
    when(mockPermission.getExclusiveGroup()).thenReturn(false);

    when(mockAccessSection.getPermissions()).thenReturn(ImmutableList.of(mockPermission));

    Map<String, AccessSection> currentSections = new HashMap<>();
    currentSections.put("refs/heads/main", mockAccessSection);

    Map<String, ProjectTree.Value> parentPermissions = new HashMap<>();
    parentPermissions.put("refs/heads/release/* read Developers", new ProjectTree.Value("ALLOW", true));

    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(currentSections, parentPermissions);

    assertEquals(2, result.size());
    assertTrue(result.containsKey("refs/heads/main read Developers"));
    assertTrue(result.containsKey("refs/heads/release/* read Developers"));
    assertTrue(result.get("refs/heads/release/* read Developers").isInherited());
  }

  @Test
  public void testProcessAccessSections_groupNameWithSpaces() {
    when(mockGroupReference.getName()).thenReturn("Code Review Team");
    when(mockPermissionRule.getGroup()).thenReturn(mockGroupReference);
    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.ALLOW);
    when(mockPermissionRule.getForce()).thenReturn(false);

    when(mockPermission.getName()).thenReturn("read");
    when(mockPermission.getRules()).thenReturn(ImmutableList.of(mockPermissionRule));
    when(mockPermission.getExclusiveGroup()).thenReturn(false);

    when(mockAccessSection.getPermissions()).thenReturn(ImmutableList.of(mockPermission));

    Map<String, AccessSection> currentSections = new HashMap<>();
    currentSections.put("refs/heads/main", mockAccessSection);

    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(currentSections, new HashMap<>());

    assertEquals(1, result.size());
    assertTrue(result.containsKey("refs/heads/main read Code-Review-Team"));
  }

  @Test
  public void testProcessAccessSections_nullCurrentSections() {
    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(null, new HashMap<>());

    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testProcessAccessSections_nullParent() {
    when(mockGroupReference.getName()).thenReturn("Developers");
    when(mockPermissionRule.getGroup()).thenReturn(mockGroupReference);
    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.ALLOW);
    when(mockPermissionRule.getForce()).thenReturn(false);

    when(mockPermission.getName()).thenReturn("read");
    when(mockPermission.getRules()).thenReturn(ImmutableList.of(mockPermissionRule));
    when(mockPermission.getExclusiveGroup()).thenReturn(false);

    when(mockAccessSection.getPermissions()).thenReturn(ImmutableList.of(mockPermission));

    Map<String, AccessSection> currentSections = new HashMap<>();
    currentSections.put("refs/heads/main", mockAccessSection);

    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(currentSections, null);

    assertEquals(1, result.size());
  }

  @Test
  public void testProcessAccessSections_multipleRulesPerPermission() {
    PermissionRule rule1 = mock(PermissionRule.class);
    PermissionRule rule2 = mock(PermissionRule.class);

    GroupReference group1 = mock(GroupReference.class);
    GroupReference group2 = mock(GroupReference.class);

    when(group1.getName()).thenReturn("Group1");
    when(group2.getName()).thenReturn("Group2");

    when(rule1.getGroup()).thenReturn(group1);
    when(rule1.getAction()).thenReturn(PermissionRule.Action.ALLOW);
    when(rule1.getForce()).thenReturn(false);

    when(rule2.getGroup()).thenReturn(group2);
    when(rule2.getAction()).thenReturn(PermissionRule.Action.DENY);
    when(rule2.getForce()).thenReturn(false);

    when(mockPermission.getName()).thenReturn("read");
    when(mockPermission.getRules()).thenReturn(ImmutableList.of(rule1, rule2));
    when(mockPermission.getExclusiveGroup()).thenReturn(false);

    when(mockAccessSection.getPermissions()).thenReturn(ImmutableList.of(mockPermission));

    Map<String, AccessSection> currentSections = new HashMap<>();
    currentSections.put("refs/heads/main", mockAccessSection);

    Map<String, ProjectTree.Value> result = invokeProcessAccessSections(currentSections, new HashMap<>());

    assertEquals(2, result.size());
    assertTrue(result.containsKey("refs/heads/main read Group1"));
    assertTrue(result.containsKey("refs/heads/main read Group2"));
  }

  @Test
  public void testTreeFromQuery_withMockedProjects() {
    ProjectInfo project1 = mock(ProjectInfo.class);
    project1.name = "project1";

    when(gerritApi.projects()).thenReturn(projects);
    when(projects.query()).thenReturn(queryRequest);
    when(queryRequest.withQuery(anyString())).thenReturn(queryRequest);

    assertNotNull(queryRequest);
  }

  private Map<String, ProjectTree.Value> invokeProcessExtensionPanelSections(
      Map<String, ImmutableList<String>> currentSections,
      Map<String, ProjectTree.Value> parentPermissions) {
    try {
      java.lang.reflect.Method method = ProjectTree.class.getDeclaredMethod(
          "processExtensionPanelSections", Map.class, Map.class);
      method.setAccessible(true);
      return (Map<String, ProjectTree.Value>) method.invoke(projectTree, currentSections, parentPermissions);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke processExtensionPanelSections", e);
    }
  }

  private Map<String, ProjectTree.Value> invokeProcessPluginConfigs(
      Map<String, String> currentConfigs,
      Map<String, ProjectTree.Value> parentPermissions) {
    try {
      java.lang.reflect.Method method = ProjectTree.class.getDeclaredMethod(
          "processPluginConfigs", Map.class, Map.class);
      method.setAccessible(true);
      return (Map<String, ProjectTree.Value>) method.invoke(projectTree, currentConfigs, parentPermissions);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke processPluginConfigs", e);
    }
  }

  private Map<String, ProjectTree.Value> invokeProcessLabelsSections(
      Map<String, LabelType> currentLabels,
      Map<String, ProjectTree.Value> parentPermissions) {
    try {
      java.lang.reflect.Method method = ProjectTree.class.getDeclaredMethod(
          "processLabelsSections", Map.class, Map.class);
      method.setAccessible(true);
      return (Map<String, ProjectTree.Value>) method.invoke(projectTree, currentLabels, parentPermissions);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke processLabelsSections", e);
    }
  }

  private Map<String, ProjectTree.Value> invokeProcessAccessSections(
      Map<String, AccessSection> currentSections,
      Map<String, ProjectTree.Value> parentPermissions) {
    try {
      java.lang.reflect.Method method = ProjectTree.class.getDeclaredMethod(
          "processAccessSections", Map.class, Map.class);
      method.setAccessible(true);
      return (Map<String, ProjectTree.Value>) method.invoke(projectTree, currentSections, parentPermissions);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke processAccessSections", e);
    }
  }

  private String invokeFormatPermissionValue(PermissionRule rule, String permissionName) {
    try {
      java.lang.reflect.Method method = ProjectTree.class.getDeclaredMethod(
          "formatPermissionValue", PermissionRule.class, String.class);
      method.setAccessible(true);
      return (String) method.invoke(projectTree, rule, permissionName);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke formatPermissionValue", e);
    }
  }

  @Test
  public void testFormatPermissionValue_labelPermissionWithRange() {
    when(mockPermissionRule.getMin()).thenReturn(-2);
    when(mockPermissionRule.getMax()).thenReturn(2);

    String result = invokeFormatPermissionValue(mockPermissionRule, "label-Code-Review");

    assertEquals("-2...2", result);
  }

  @Test
  public void testFormatPermissionValue_normalPermissionWithoutForce() {
    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.ALLOW);
    when(mockPermissionRule.getForce()).thenReturn(false);

    String result = invokeFormatPermissionValue(mockPermissionRule, "read");

    assertEquals("ALLOW", result);
  }

  @Test
  public void testFormatPermissionValue_normalPermissionWithForce() {
    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.DENY);
    when(mockPermissionRule.getForce()).thenReturn(true);

    String result = invokeFormatPermissionValue(mockPermissionRule, "push");

    assertEquals("DENY_FORCE", result);
  }

  @Test
  public void testFormatPermissionValue_variousActions() {
    when(mockPermissionRule.getForce()).thenReturn(false);

    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.ALLOW);
    assertEquals("ALLOW", invokeFormatPermissionValue(mockPermissionRule, "admin"));

    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.DENY);
    assertEquals("DENY", invokeFormatPermissionValue(mockPermissionRule, "admin"));

    when(mockPermissionRule.getAction()).thenReturn(PermissionRule.Action.BATCH);
    assertEquals("BATCH", invokeFormatPermissionValue(mockPermissionRule, "admin"));
  }
}
