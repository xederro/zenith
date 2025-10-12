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

package tech.xederro.zenith;

import com.google.gerrit.extensions.api.projects.ProjectInput;
import com.google.gerrit.extensions.client.SubmitType;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.kohsuke.args4j.Option;

import java.util.Arrays;
import java.util.stream.Collectors;

// SSH command for project creation from templates
public class CreateTemplateCommand extends TemplateCommand {
  private final ProjectInput input;


  @Inject
  public CreateTemplateCommand(FileRepoHelper fileRepoHelper, Gson gson, ProjectInput input) {
    super(fileRepoHelper, gson);
    this.input = input;
  }

  // Command options - populates project input fields from CLI arguments

  @Option(
      name = "--parent",
      aliases = {"-p"},
      metaVar = "PARENT",
      usage = "The name of the parent project. If not set, the All-Projects project will be the parent project.")
  public void setParent(String parent) {
    this.input.parent = parent;
  }

  @Option(
      name = "--desc",
      aliases = {"-d"},
      metaVar = "DESC",
      usage = "The description of the project.")
  public void setDesc(String description) {
    this.input.description = description;
  }

  @Option(
      name = "--permissions-only",
      aliases = {"-po"},
      metaVar = "PERMISSIONS-ONLY",
      usage = "Whether a permission-only project should be created.")
  public void setPermissionsOnly(Boolean permissionsOnly) {
    this.input.permissionsOnly = permissionsOnly;
  }

  @Option(
      name = "--submit-type",
      aliases = {"-st"},
      metaVar = "SUBMIT-TYPE",
      usage = """
          The submit type that should be set for the project (MERGE_IF_NECESSARY, REBASE_IF_NECESSARY, REBASE_ALWAYS, FAST_FORWARD_ONLY, MERGE_ALWAYS, CHERRY_PICK).
          If not set, MERGE_IF_NECESSARY is set as submit type unless repository.<name>.defaultSubmitType is set to a different value.""")
  public void setSubmitType(String submitType) {
    this.input.submitType = SubmitType.valueOf(submitType.toUpperCase());
  }

  @Option(
      name = "--branches",
      aliases = {"-b"},
      metaVar = "BRANCHES",
      usage = """
          A list of branches that should be initially created.
          For the branch names the refs/heads/ prefix can be omitted.
          The first entry of the list will be the default branch.
          If the list is empty, host-level default is used.
          Branches in the Gerrit internal ref space are not allowed, such as refs/groups/, refs/changes/, etc…""")
  public void setBranches(String branches) {
    this.input.branches = Arrays.stream(branches.split(",")).map(String::trim).collect(Collectors.toList());
  }

  @Option(
      name = "--owners",
      aliases = {"-o"},
      metaVar = "OWNERS",
      usage = """
          A list of groups that should be assigned as project owner.
          Each group in the list must be specified as group-id.
          If not set, the groups that are configured as default owners are set as project owners.""")
  public void setOwners(String owners) {
    this.input.owners = Arrays.stream(owners.split(",")).map(String::trim).collect(Collectors.toList());
  }

  @Option(
      name = "--max-object-size-limit",
      aliases = {"-mosl"},
      metaVar = "MAX-OBJECT-SIZE-LIMIT",
      usage = "Max allowed Git object size for this project. Common unit suffixes of 'k', 'm', or 'g' are supported.")
  public void setMaxObjectSizeLimit(String maxObjectSizeLimit) {
    this.input.maxObjectSizeLimit = maxObjectSizeLimit;
  }

  // Main entry point for the SSH command
  @Override
  protected void run() {
    try {
      // Always create an empty commit when creating project
      this.input.createEmptyCommit = true;
      this.input.name = super.name;
      // Create the repository
      fileRepoHelper.createRepo(this.input);
      stdout.println("Created project " + this.input.name);
      super.run();
    } catch (Exception e) {
      stdout.println("error: " + e.getMessage());
    }
  }
}
