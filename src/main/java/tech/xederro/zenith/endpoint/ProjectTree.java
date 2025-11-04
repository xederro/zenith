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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.entities.*;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.ConfigInfo;
import com.google.gerrit.extensions.client.InheritableBoolean;
import com.google.gerrit.extensions.client.ProjectState;
import com.google.gerrit.extensions.client.SubmitType;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectTree {
  private final GerritApi gerritApi;
  private final ProjectCache projectCache;

  @Inject
  ProjectTree(GerritApi gerritApi, ProjectCache projectCache) {
    this.gerritApi = gerritApi;
    this.projectCache = projectCache;
  }

  public ProjectData treeFromQuery(String query) throws RestApiException {
    List<ProjectInfo> projectInfoList = gerritApi.projects().query().withQuery(query).get();
    return buildTree(projectInfoList);
  }

  private ProjectData buildTree(List<ProjectInfo> projectInfoList) {
    Map<String, ProjectData> projectMap = new HashMap<>();

    for (ProjectInfo data : projectInfoList) {
      fillChildren(projectMap, data);
    }

    // build tree
    List<ProjectData> roots = new ArrayList<>();
    projectMap.keySet().stream().sorted().forEach(projectName -> {
      ProjectData data = projectMap.get(projectName);
      if (data.parent != null && projectMap.containsKey(data.parent)) {
        projectMap.get(data.parent).children.add(data);
      } else {
        roots.add(data);
      }
    });

    // fill with data
    for (ProjectData project : roots) {
      fillWithData(project, null);
    }

    if (roots.size() == 1) {
      return roots.getFirst();
    } else {
      return new ProjectData("root", null, roots, null);
    }
  }

  private void fillChildren(Map<String, ProjectData> projectMap, ProjectInfo node) {
    if (node == null || projectMap.containsKey(node.name)) {
      return; // already processed or invalid node
    }

    // create node
    projectMap.put(node.name, new ProjectData(node.name, node.parent, new ArrayList<>(), null));

    // process parent
    ProjectInfo parentInfo = null;
    if (node.parent != null) {
      try {
        parentInfo = gerritApi.projects().name(node.parent).get();
      } catch (RestApiException ignored) {}
    }
    fillChildren(projectMap, parentInfo);
  }

  private void fillWithData(ProjectData node, Map<String, Value> parentProcessedPermissions) {
    Map<String, Value> val = new HashMap<>();
    Map<String, AccessSection> currentAccessSections;
    Map<String, LabelType> currentLabelsSections;
    Map<String, String> currentPluginConfigs;
    Map<String, ImmutableList<String>> currentExtensionPanelSections;
    try {
      val.put("parent", new Value(node.parent, false));
      val.putAll(getConfigInfo(gerritApi.projects().name(node.name).config()));

      CachedProjectConfig cachedConfig = projectCache.get(Project.nameKey(node.name))
          .orElseThrow(ProjectCache.illegalState(Project.nameKey(node.name))).getConfig();

      currentAccessSections = cachedConfig.getAccessSections();
      currentLabelsSections = cachedConfig.getLabelSections();
      currentPluginConfigs = cachedConfig.getPluginConfigs();
      currentExtensionPanelSections = cachedConfig.getExtensionPanelSections();

      Map<String, Value> accessValues = processAccessSections(currentAccessSections, parentProcessedPermissions);
      val.putAll(accessValues);

      Map<String, Value> labelValues = processLabelsSections(currentLabelsSections, parentProcessedPermissions);
      val.putAll(labelValues);

      Map<String, Value> processValues = processPluginConfigs(currentPluginConfigs, parentProcessedPermissions);
      val.putAll(processValues);

      Map<String, Value> processExtensionPanelSections = processExtensionPanelSections(currentExtensionPanelSections, parentProcessedPermissions);
      val.putAll(processExtensionPanelSections);

    } catch (Exception ignored) {}

    node.values = val;

    Map<String, Value> processedPermissions = val.entrySet().stream()
        .filter(e -> e.getKey().contains(" ") && e.getKey().split(" ").length >= 3)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    for (ProjectData child : node.children) {
      fillWithData(child, processedPermissions);
    }
  }

  private Map<String, Value> processExtensionPanelSections(
      Map<String, ImmutableList<String>> currentExtensionPanelSections,
      Map<String, Value> parentProcessedPermissions) {
    Map<String, Value> result = new HashMap<>();
    if (currentExtensionPanelSections == null) {
      currentExtensionPanelSections = new HashMap<>();
    }
    if (parentProcessedPermissions == null) {
      parentProcessedPermissions = new HashMap<>();
    }
    for (Map.Entry<String, ImmutableList<String>> entry : currentExtensionPanelSections.entrySet()) {
      String panelName = entry.getKey();
      ImmutableList<String> panelValues = entry.getValue();

      String key = "extension_panel " + panelName;
      String value = String.join(",", panelValues);

      result.put(key, new Value(value, false));
    }

    for (Map.Entry<String, Value> entry : parentProcessedPermissions.entrySet()) {
      String key = entry.getKey();

      if (!key.startsWith("extension_panel ")) {
        continue;
      }

      boolean isOverridden = result.containsKey(key);

      if (!isOverridden) {
        result.put(key, new Value(entry.getValue().value(), true));
      }
    }

    return result;
  }

  private Map<String, Value> processPluginConfigs(
      Map<String, String> currentPluginConfigs,
      Map<String, Value> parentProcessedPermissions) {
    Map<String, Value> result = new HashMap<>();

    if (currentPluginConfigs == null) {
      currentPluginConfigs = new HashMap<>();
    }

    if (parentProcessedPermissions == null) {
      parentProcessedPermissions = new HashMap<>();
    }

    for (Map.Entry<String, String> entry : currentPluginConfigs.entrySet()) {
      String pluginKey = entry.getKey();
      String pluginValue = entry.getValue();

      String key = "plugin " + pluginKey;
      result.put(key, new Value(pluginValue, false));
    }

    for (Map.Entry<String, Value> entry : parentProcessedPermissions.entrySet()) {
      String key = entry.getKey();

      if (!key.startsWith("plugin ")) {
        continue;
      }

      boolean isOverridden = result.containsKey(key);

      if (!isOverridden) {
        result.put(key, new Value(entry.getValue().value(), true));
      }
    }

    return result;
  }

  private Map<String, Value> processLabelsSections(
      Map<String, LabelType> currentLabelsSections,
      Map<String, Value> parentProcessedPermissions) {

    Map<String, Value> result = new HashMap<>();
    if (currentLabelsSections == null) {
      currentLabelsSections = new HashMap<>();
    }
    if (parentProcessedPermissions == null) {
      parentProcessedPermissions = new HashMap<>();
    }

    for (Map.Entry<String, LabelType> entry : currentLabelsSections.entrySet()) {
      String labelName = entry.getKey();
      LabelType labelType = entry.getValue();

      List<String> refPatterns = labelType.getRefPatterns();

      List<String> patterns = (refPatterns != null && !refPatterns.isEmpty())
          ? refPatterns
          : List.of("*");

      for (String refPattern : patterns) {
        String key = "label " + labelName + " " + refPattern;

        LabelValue min = labelType.getMin();
        LabelValue max = labelType.getMax();

        String value;
        if (min != null && max != null) {
          value = min.getValue() + "..." + max.getValue();
        } else if (min != null) {
          value = String.valueOf(min.getValue());
        } else if (max != null) {
          value = String.valueOf(max.getValue());
        } else {
          value = "0...0";
        }

        result.put(key + " label-range", new Value(value, false));
        result.put(key + " label-function", new Value(labelType.getFunction().getFunctionName(), false));

        labelType.getCopyCondition().ifPresentOrElse(
            condition ->  result.put(key + " label-copy-condition", new Value(condition, false)),
            () -> result.put(key + " label-copy-condition", new Value("NONE", false)));


        result.put(key + " label-default-value", new Value(Short.valueOf(labelType.getDefaultValue()).toString(), false));
        result.put(key + " label-ignore-self-approval", new Value(Boolean.valueOf(labelType.isIgnoreSelfApproval()).toString().toUpperCase(), false));
        result.put(key + " label-allow-post-submit", new Value(Boolean.valueOf(labelType.isAllowPostSubmit()).toString().toUpperCase(), false));
      }
    }

    for (Map.Entry<String, Value> entry : parentProcessedPermissions.entrySet()) {
      String key = entry.getKey();

      if (!key.startsWith("label ")) {
        continue;
      }

      boolean isOverridden = result.containsKey(key);

      if (!isOverridden) {
        result.put(key, new Value(entry.getValue().value(), true));
      }
    }

    return result;
  }

  private Map<String, Value> processAccessSections(
      Map<String, AccessSection> currentSections,
      Map<String, Value> parentProcessedPermissions) {

    Map<String, Value> result = new HashMap<>();

    if (currentSections == null) {
      currentSections = new HashMap<>();
    }
    if (parentProcessedPermissions == null) {
      parentProcessedPermissions = new HashMap<>();
    }

    // Process current sections and track exclusive groups to handle removals
    Map<String, Set<String>> exclusiveGroups = new HashMap<>();
    for (Map.Entry<String, AccessSection> entry : currentSections.entrySet()) {
      String accessName = entry.getKey();
      AccessSection section = entry.getValue();

      for (Permission permission : section.getPermissions()) {
        String permissionName = permission.getName();
        String key = accessName + " " + permissionName;

        for (PermissionRule rule : permission.getRules()) {
          String groupName = rule.getGroup().getName().replace(" ", "-");
          String fullKey = key + " " + groupName;

          String value = formatPermissionValue(rule, permissionName);
          result.put(fullKey, new Value(value, false));
        }

        // add exclusive groups for later processing
        if (permission.getExclusiveGroup()) {
          exclusiveGroups.put(key, new HashSet<>());

          for (PermissionRule rule : permission.getRules()) {
            exclusiveGroups.get(key).add(rule.getGroup().getName());
          }
        }
      }
    }

    // Process inherited permissions from parent
    for (Map.Entry<String, Value> entry : parentProcessedPermissions.entrySet()) {
      String key = entry.getKey();

      String[] parts = key.split(" ", 3);
      if (parts.length < 3) {
        continue;
      }

      String accessName = parts[0];
      String permissionName = parts[1];
      String exclusiveKey = accessName + " " + permissionName;

      boolean isOverridden = result.containsKey(key);
      boolean isBlockedByExclusive = exclusiveGroups.containsKey(exclusiveKey);

      // if they are overridden or blocked skip adding
      if (!isOverridden && !isBlockedByExclusive) {
        result.put(key, new Value(entry.getValue().value(), true));
      }
    }

    return result;
  }


  private String formatPermissionValue(PermissionRule rule, String permissionName) {
    StringBuilder value = new StringBuilder();

    if (permissionName.startsWith("label-")) {
      value.append(rule.getMin()).append("...").append(rule.getMax());
    } else {
      value.append(rule.getAction().toString());

      if (rule.getForce()) {
        value.append("_FORCE");
      }
    }

    return value.toString();
  }

  private Map<String, Value> getConfigInfo(ConfigInfo configInfo) throws IllegalStateException {
    Map<String, Value> values = new HashMap<>();
    values.put("use_contributor_agreements", parseInherited(configInfo.useContributorAgreements));
    values.put("use_content_merge", parseInherited(configInfo.useContentMerge));
    values.put("use_signed_off_by", parseInherited(configInfo.useSignedOffBy));
    values.put("create_new_change_for_all_not_in_target", parseInherited(configInfo.createNewChangeForAllNotInTarget));
    values.put("require_change_id", parseInherited(configInfo.requireChangeId));
    values.put("enable_signed_push", parseInherited(configInfo.enableSignedPush));
    values.put("require_signed_push", parseInherited(configInfo.requireSignedPush));
    values.put("reject_implicit_merges", parseInherited(configInfo.rejectImplicitMerges));
    values.put("private_by_default", parseInherited(configInfo.privateByDefault));
    values.put("work_in_progress_by_default", parseInherited(configInfo.workInProgressByDefault));
    values.put("enable_reviewer_by_email", parseInherited(configInfo.enableReviewerByEmail));
    values.put("match_author_to_committer_date", parseInherited(configInfo.matchAuthorToCommitterDate));
    values.put("reject_empty_commit", parseInherited(configInfo.rejectEmptyCommit));
    values.put("skip_adding_author_and_committer_as_reviewers", parseInherited(configInfo.skipAddingAuthorAndCommitterAsReviewers));

    ConfigInfo.SubmitTypeInfo defaultSubmitType = configInfo.defaultSubmitType;
    if (defaultSubmitType.configuredValue == SubmitType.INHERIT) {
      values.put("default_submit_type", new Value(defaultSubmitType.inheritedValue.name(), true));
    } else {
      values.put("default_submit_type", new Value(defaultSubmitType.configuredValue.name(), false));
    }

    ConfigInfo.MaxObjectSizeLimitInfo maxObjectSizeLimit = configInfo.maxObjectSizeLimit;
    if (maxObjectSizeLimit == null || maxObjectSizeLimit.value == null) {
      values.put("max_object_size_limit", new Value("NOT_AVAILABLE", false));
    } else {
      values.put("max_object_size_limit", new Value(maxObjectSizeLimit.configuredValue, false));
    }

    ProjectState state = configInfo.state;
    if (state == null) {
      values.put("state", new Value("NOT_AVAILABLE", false));
    } else {
      values.put("state", new Value(state.name(), false));
    }

    return values;
  }

  private Value parseInherited(ConfigInfo.InheritedBooleanInfo info) throws IllegalStateException {
    if (info == null) {
      return new Value("NOT_AVAILABLE", false);
    }
    try {
      if (info.configuredValue == InheritableBoolean.INHERIT) {
        return new Value(info.inheritedValue.toString().toUpperCase(), true);
      } else {
        return new Value(info.configuredValue.name(), false);
      }
    } catch (Exception e) {
      return new Value("NOT_AVAILABLE", false);
    }
  }

  public static final class ProjectData implements Comparable<ProjectData> {
    public String name;
    public String parent;
    public Map<String, Value> values;
    public List<ProjectData> children;

    public ProjectData(String name, String parent, List<ProjectData> children, Map<String, Value> values) {
      this.name = name;
      this.parent = parent;
      this.values = values;
      this.children = children;
    }

    @Override
    public int compareTo(ProjectData o) {
      return this.name.compareTo(o.name);
    }
  }

  public record Value(String value, Boolean isInherited) {}
}
