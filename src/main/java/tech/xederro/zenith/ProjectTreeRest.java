package tech.xederro.zenith;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectTreeRest implements RestReadView<ConfigResource> {
  private final GerritApi gerritApi;

  @Inject
  ProjectTreeRest(GerritApi gerritApi) {
    this.gerritApi = gerritApi;
  }

  public Project buildTree(List<ProjectInfo> projectInfoList) {
    Map<String, Project> projectMap = new HashMap<>();

    for (ProjectInfo data : projectInfoList) {
      projectMap.put(data.name, new Project(data.name, data.parent, new ArrayList<>()));
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
      return roots.get(0);
    } else {
      Project root = new Project("root", null, roots);
      return root;
    }
  }

  @Override
  public Response<Project> apply(ConfigResource resource) throws AuthException, BadRequestException, ResourceConflictException, Exception {
    List<ProjectInfo> projectInfoList = gerritApi.projects().query().get();
    Project root = buildTree(projectInfoList);
    return Response.ok(root);
  }

  public record Project(String name, String value, List<Project> children) {}
}
