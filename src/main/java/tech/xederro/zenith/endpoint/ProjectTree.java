package tech.xederro.zenith.endpoint;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.ConfigInfo;
import com.google.gerrit.extensions.client.InheritableBoolean;
import com.google.gerrit.extensions.client.ProjectState;
import com.google.gerrit.extensions.client.SubmitType;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.*;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectTree {
  private final GerritApi gerritApi;
//  private final ProjectCache projectCache;

  @Inject
  ProjectTree(
      GerritApi gerritApi
//      , ProjectCache projectCache
  ) {
    this.gerritApi = gerritApi;
//    this.projectCache = projectCache;
  }

  public ProjectAccess treeFromQuery(String query, String config) throws RestApiException {
     List<ProjectInfo> projectInfoList = gerritApi.projects().query().withQuery(query).get();
    return buildTree(projectInfoList, config);
  }

  private ProjectAccess buildTree(List<ProjectInfo> projectInfoList, String config) {
    Map<String, ProjectAccess> projectMap = new HashMap<>();

    for (ProjectInfo data : projectInfoList) {
      fillChildren(projectMap, data, config);
    }

    List<ProjectAccess> roots = new ArrayList<>();
    projectMap.keySet().stream().sorted().forEach(projectName -> {
      ProjectAccess data = projectMap.get(projectName);
      if (data.parent != null && projectMap.containsKey(data.parent)) {
        projectMap.get(data.parent).children().add(data);
      } else {
        roots.add(data);
      }
    });

    if (roots.size() == 1) {
      return roots.getFirst();
    } else {
      return new ProjectAccess("root", null, null, roots);
    }
  }

  private void fillChildren(Map<String, ProjectAccess> projectMap, ProjectInfo node, String config) {
    if (node == null || projectMap.containsKey(node.name)) {
      return; // already processed or invalid node
    }

    // process node
    Value val;
//    Map<String, AccessSection> accessSections = null;
    try {
      if (config != null && !config.equals("parent")) {
        val = getConfigInfo(gerritApi.projects().name(node.name).config(), config);
      } else {
        val = new Value(node.parent, false);
      }
//      com.google.gerrit.server.project.ProjectState projectState = projectCache.get(Project.nameKey(node.name))
//          .orElseThrow(ProjectCache.illegalState(Project.nameKey(node.name)));
//
//      accessSections = projectState.getConfig().getAccessSections();
    } catch (Exception e) {
      val = new Value("ERROR", false);
    }

    projectMap.put(node.name, new ProjectAccess(node.name, node.parent, val, new ArrayList<>()));

    // process parent
    ProjectInfo parentInfo = null;
    if (node.parent != null) {
      try {
        parentInfo = gerritApi.projects().name(node.parent).get();
      } catch (RestApiException ignored) {}
    }
    fillChildren(projectMap, parentInfo, config);
  }

  private Value getConfigInfo(ConfigInfo configInfo, String config) throws IllegalStateException {
    ConfigInfo.InheritedBooleanInfo info;
    switch (config) {
      case "use_contributor_agreements" -> info = configInfo.useContributorAgreements;
      case "use_content_merge" -> info = configInfo.useContentMerge;
      case "use_signed_off_by" -> info = configInfo.useSignedOffBy;
      case "create_new_change_for_all_not_in_target" -> info = configInfo.createNewChangeForAllNotInTarget;
      case "require_change_id" -> info = configInfo.requireChangeId;
      case "enable_signed_push" -> info = configInfo.enableSignedPush;
      case "require_signed_push" -> info = configInfo.requireSignedPush;
      case "reject_implicit_merges" -> info = configInfo.rejectImplicitMerges;
      case "private_by_default" -> info = configInfo.privateByDefault;
      case "work_in_progress_by_default" -> info = configInfo.workInProgressByDefault;
      case "enable_reviewer_by_email" -> info = configInfo.enableReviewerByEmail;
      case "match_author_to_committer_date" -> info = configInfo.matchAuthorToCommitterDate;
      case "reject_empty_commit" -> info = configInfo.rejectEmptyCommit;
      case "skip_adding_author_and_committer_as_reviewers" -> info = configInfo.skipAddingAuthorAndCommitterAsReviewers;
      case "default_submit_type" -> {
        ConfigInfo.SubmitTypeInfo defaultSubmitType = configInfo.defaultSubmitType;
        if (defaultSubmitType.configuredValue == SubmitType.INHERIT) {
          return new Value(defaultSubmitType.inheritedValue.name(), true);
        } else {
          return new Value(defaultSubmitType.configuredValue.name(), false);
        }
      }
      case "max_object_size_limit" -> {
        ConfigInfo.MaxObjectSizeLimitInfo maxObjectSizeLimit = configInfo.maxObjectSizeLimit;
        if (maxObjectSizeLimit == null || maxObjectSizeLimit.value == null) {
          return new Value("NOT_SET", false);
        } else {
          return new Value(maxObjectSizeLimit.configuredValue, false);
        }
      }
      case "project_state" -> {
        ProjectState state = configInfo.state;
        if (state == null) {
          return new Value("NOT_AVAILABLE", false);
        } else {
          return new Value(state.name(), false);
        }
      }
      default -> throw new IllegalStateException("Unexpected value: " + config);
      // TODO: plugins!
    }
    if (info == null) {
      return new Value("NOT_AVAILABLE", false);
    }
    if (info.configuredValue == InheritableBoolean.INHERIT) {
      return new Value(info.inheritedValue.toString().toUpperCase(), true);
    } else {
      return new Value(info.configuredValue.name(), false);
    }
  }

  public record ProjectAccess(String name, String parent, Value value, List<ProjectAccess> children) implements Comparable<ProjectAccess> {
    @Override
    public int compareTo(ProjectAccess o) {
      return this.name.compareTo(o.name);
    }
  }
  public record Value(String value, Boolean isInherited) {}
}
