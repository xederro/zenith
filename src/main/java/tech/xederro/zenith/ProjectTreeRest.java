package tech.xederro.zenith;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.ConfigInfo;
import com.google.gerrit.extensions.client.InheritableBoolean;
import com.google.gerrit.extensions.client.ProjectState;
import com.google.gerrit.extensions.client.SubmitType;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectTreeRest implements RestReadView<ConfigResource> {
  private final GerritApi gerritApi;

  private String query;
  private String config;

  @Inject
  ProjectTreeRest(GerritApi gerritApi) {
    this.gerritApi = gerritApi;
  }

  @Option(name = "--config", metaVar = "CONFIG")
  private void config(String arg) {
    this.config = arg;
  }

  @Option(name = "--query", metaVar = "QUERY")
  private void query(String arg) {
    this.query = arg;
  }

  @Override
  public Response<Project> apply(ConfigResource resource) throws AuthException, BadRequestException, ResourceConflictException, Exception {
    List<ProjectInfo> projectInfoList = gerritApi.projects().query().withQuery(this.query).get();
    Project root = buildTree(projectInfoList);
    return Response.ok(root);
  }

  public Project buildTree(List<ProjectInfo> projectInfoList) {
    Map<String, Project> projectMap = new HashMap<>();

    for (ProjectInfo data : projectInfoList) {
      Value val;
      try {
        if (config != null) {
          val = getConfigInfo(gerritApi.projects().name(data.name).config());
        } else {
          val = new Value(data.parent, false);
        }
      } catch (Exception e) {
        val = new Value("ERROR", false);
      }
      /* Idea TODO: create actual info in value for the query created!
      * @Inject
      * PermissionBackend permissionBackend;
      *
      * PermissionBackend.ForProject forProject = permissionBackend.project(Project.nameKey("test"));
      * forProject.ref("refs/heads/master").testOrFalse(Permission.PUSH);
      * */
      projectMap.put(data.name, new Project(data.name, val, new ArrayList<>()));
    }

    List<Project> roots = new ArrayList<>();
    for (ProjectInfo data : projectInfoList) {
      Project project = projectMap.get(data.name);
      if (data.parent != null && projectMap.containsKey(data.parent)) {
        projectMap.get(data.parent).children().add(project);
      } else {
        roots.add(project);
      }
    }

    if (roots.size() == 1) {
      return roots.getFirst();
    } else {
      return new Project("root", null, roots);
    }
  }

  private Value getConfigInfo(ConfigInfo configInfo) throws IllegalStateException {
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
      default -> {
        switch (config) {
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
      }
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

  public record Project(String name, Value value, List<Project> children) {}
  public record Value(String value, Boolean isInherited) {}
}
