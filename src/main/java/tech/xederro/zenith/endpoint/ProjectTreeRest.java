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

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

public class ProjectTreeRest implements RestReadView<ConfigResource> {
  private final ProjectTree projectTree;

  private String query;

  @Inject
  ProjectTreeRest(ProjectTree projectTree) {
    this.projectTree = projectTree;
  }

  @Option(name = "--query", metaVar = "QUERY")
  private void query(String arg) {
    this.query = arg;
  }

  @Override
  public Response<ProjectTree.ProjectData> apply(ConfigResource resource) throws AuthException, BadRequestException, ResourceConflictException, Exception {
    return Response.ok(projectTree.treeFromQuery(query));
  }
}
