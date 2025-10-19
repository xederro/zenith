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

import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CommandMetaData(
    name = "apply-template-command",
    description = "SSH command for applying templates to repositories")
public class ApplyTemplateCommand extends SshCommand {
  protected final FileRepoHelper fileRepoHelper;
  protected final Gson gson;

  protected List<String> targets;
  protected Object json;

  @Inject
  public ApplyTemplateCommand(FileRepoHelper fileRepoHelper, Gson gson) {
    this.fileRepoHelper = fileRepoHelper;
    this.gson = gson;
  }

  // Command options - populates project input fields from CLI arguments

  @Argument(index = 0, metaVar = "NAME", usage = "name of project to be created")
  protected String projectName;

  @Option(
      name = "--template-targets",
      aliases = {"-tt"},
      metaVar = "TEMPLATE-TARGETS",
      usage = "from@ref:to,from@ref:to")
  public void setTargets(String targets) {
    this.targets = Arrays.stream(targets.split(",")).map(String::trim).collect(Collectors.toList());
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
      // If target templates are specified, create corresponding commits
      if (targets != null) {
        for (String target : targets) {
          fileRepoHelper.createCommit(this.projectName, target, json);
        }
      }
      stdout.println("Applied template to " + this.projectName);
    } catch (Exception e) {
      stderr.println("error: " + e.getMessage());
    }
  }
}
