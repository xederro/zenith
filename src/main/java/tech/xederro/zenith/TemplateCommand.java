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

import com.google.gerrit.sshd.SshCommand;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.eclipse.jgit.lib.*;
import org.kohsuke.args4j.Option;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// SSH command for applying templates to repositories
public class TemplateCommand extends SshCommand {
  protected final FileRepoHelper fileRepoHelper;
  protected final Gson gson;

  protected List<String> targets;
  protected Object json;
  protected String name;

  @Inject
  public TemplateCommand(FileRepoHelper fileRepoHelper, Gson gson) {
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
    this.name = name;
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
          fileRepoHelper.createCommit(this.name, target, json);
        }
      }
      stdout.println("Applied template " + this.name);
    } catch (Exception e) {
      stdout.println("error: " + e.getMessage());
    }
  }
}
