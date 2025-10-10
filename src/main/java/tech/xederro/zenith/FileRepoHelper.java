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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.ProjectInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Helper class to manage repositories and handle template commits
public class FileRepoHelper {
  private final GerritApi gerritApi;
  private final GitRepositoryManager repoManager;
  private final Handlebars engine;

  // Dependency injection constructor
  @Inject
  public FileRepoHelper(GerritApi gerritApi, GitRepositoryManager repoManager, Handlebars engine) {
    this.gerritApi = gerritApi;
    this.repoManager = repoManager;
    this.engine = engine;
  }

  // Create a new Gerrit project using the input specification
  public void createRepo(ProjectInput input) throws RestApiException {
    gerritApi.projects().create(input);
  }

  // Create a commit on a given branch, applying a Handlebars template
  public void createCommit(String projectTo, String target, Object json) throws Exception {
    // Parse the target argument into repo, ref, and destination branch
    String[] fromRefTo = target.split("[:@]");
    String from = fromRefTo[0];
    String ref = fromRefTo[1];
    String to = fromRefTo[2];

    if (!to.equals("refs/meta/config")) {
      to = Constants.R_HEADS + to;
    }

    if (!ref.equals("refs/meta/config")) {
      ref = Constants.R_HEADS + ref;
    }

    try (Repository repo = repoManager.openRepository(Project.nameKey(projectTo))) {
      ObjectInserter inserter = repo.newObjectInserter();

      // Open an in-memory index and editor for staged changes
      DirCache dc = DirCache.newInCore();
      DirCacheEditor editor = dc.editor();

      // Walk source repo and apply template logic
      walkRepo(from, ref, editor, inserter, json);

      editor.finish();
      ObjectId treeId = dc.writeTree(inserter);

      // Construct commit author/committer details
      PersonIdent ident = new PersonIdent("Zenith", "zenith@notavailable.com");
      CommitBuilder commit = new CommitBuilder();
      commit.setTreeId(treeId);
      commit.setMessage("Initial commit by Zenith");
      commit.setAuthor(ident);
      commit.setCommitter(ident);

      ObjectId commitId = inserter.insert(commit);
      inserter.flush();

      // Update or create the specified branch to point to new commit
      RefUpdate refUpdate = repo.updateRef(to);
      refUpdate.setNewObjectId(commitId);
      refUpdate.setForceUpdate(true);
      RefUpdate.Result result = refUpdate.update();

      // Check for update errors
      if (result != RefUpdate.Result.NEW && result != RefUpdate.Result.FORCED && result != RefUpdate.Result.FAST_FORWARD) {
        throw new RuntimeException("Failed to update ref: " + result.name());
      }
    }
  }

  // Recursively walk a tree, apply a template to each file, and stage results in index
  private void walkRepo(String repoName, String ref, DirCacheEditor editor, ObjectInserter inserter, Object json) throws IOException {
    try (Repository repo = repoManager.openRepository(Project.nameKey(repoName))) {
      ObjectId revId = repo.resolve(ref);
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commit = revWalk.parseCommit(revId);
        RevTree tree = commit.getTree();
        try (TreeWalk treeWalk = new TreeWalk(repo)) {
          treeWalk.addTree(tree);
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            if (treeWalk.getFileMode(0).equals(FileMode.REGULAR_FILE)) {
              ObjectId objectId = treeWalk.getObjectId(0);
              try (ObjectReader reader = repo.newObjectReader()) {
                // Read file data and process as Handlebars template
                byte[] data = reader.open(objectId).getBytes();
                Template template = engine.compileInline(new String(data, StandardCharsets.UTF_8));
                String content = template.apply(json);
                ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, content.getBytes());
                String filePath = treeWalk.getPathString();
                editor.add(new DirCacheEditor.PathEdit(filePath) {
                  @Override
                  public void apply(DirCacheEntry ent) {
                    ent.setFileMode(FileMode.REGULAR_FILE);
                    ent.setObjectId(blobId);
                  }
                });
              }
            }
          }
        }
      }
    }
  }
}
