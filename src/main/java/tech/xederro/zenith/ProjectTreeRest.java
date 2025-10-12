package tech.xederro.zenith;

import com.google.gerrit.extensions.api.GerritApi;
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

  @Inject
  ProjectTreeRest(GerritApi gerritApi) {
    this.gerritApi = gerritApi;
  }

  @Option(name = "--query", metaVar = "QUERY")
  private void parse(String arg) {
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
      /* Idea TODO: create actual info in value for the query created!
      * @Inject
      * PermissionBackend permissionBackend;
      *
      * PermissionBackend.ForProject forProject = permissionBackend.project(Project.nameKey("test"));
      * forProject.ref("refs/heads/master").testOrFalse(Permission.PUSH);
      * */
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
      return roots.getFirst();
    } else {
      return new Project("root", null, roots);
    }
  }

  public record Project(String name, String value, List<Project> children) {}
}
