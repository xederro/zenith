package tech.xederro.zenith.endpoint;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

public class ProjectTreeRest implements RestReadView<ConfigResource> {
  private final ProjectTree projectTree;

  private String query;
  private String config;

  @Inject
  ProjectTreeRest(ProjectTree projectTree) {
    this.projectTree = projectTree;
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
  public Response<ProjectTree.ProjectAccess> apply(ConfigResource resource) throws AuthException, BadRequestException, ResourceConflictException, Exception {
    return Response.ok(projectTree.treeFromQuery(query, config));
  }
}
