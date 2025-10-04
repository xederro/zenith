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
import com.google.gerrit.sshd.SshCommand;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.eclipse.jgit.lib.*;
import org.kohsuke.args4j.Option;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// SSH command for project creation from templates
public class TemplateCommand extends SshCommand {
  private final ProjectInput input;
  private final FileRepoHelper fileRepoHelper;
  private final Gson gson;

  private List<String> targets;
  private Object json;

  @Inject
  public TemplateCommand(ProjectInput input, FileRepoHelper fileRepoHelper, Gson gson) {
    this.input = input;
    this.fileRepoHelper = fileRepoHelper;
    this.gson = gson;
  }

  // Command options - populates project input fields from CLI arguments

  @Option(
      name = "--name",
      aliases = {"-n"},
      metaVar = "NAME",
      usage = """
          The name of the project (not encoded).
          If set, must match the project name in the URL.
          If name ends with .git the suffix will be automatically removed.""",
      required = true)
  public void setName(String name) {
    this.input.name = name;
  }

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
      name = "--targets",
      aliases = {"-t"},
      metaVar = "TARGETS",
      usage = "from@ref:to,from@ref:to")
  public void setTargets(String targets) {
    this.targets = Arrays.stream(targets.split(",")).map(String::trim).collect(Collectors.toList());
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

  @Option(
      name = "--json",
      aliases = {"-j"},
      metaVar = "JSON",
      usage = "A JSON that contains the input values for templates.")
  public void setJson(String json) {
    this.json = gson.fromJson(json, Object.class);
  }

  // Main entry point for the SSH command
  @Override
  protected void run() {
    try {
      // Always create an empty commit when creating project
      this.input.createEmptyCommit = true;
      // Create the repository
      fileRepoHelper.createRepo(this.input);
      // If target templates are specified, create corresponding commits
      if (targets != null) {
        for (String target : targets) {
          fileRepoHelper.createCommit(this.input.name, target, json);
        }
      }
      stdout.println("Created project " + this.input.name);
    } catch (Exception e) {
      stdout.println("error: " + e.getMessage());
    }
  }
}
