package org.sonatype.nexus.repository.composer.internal;

import com.google.common.base.Preconditions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.repository.config.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


public class GitUtils {
  protected final Logger log = Preconditions.checkNotNull(Loggers.getLogger(this));

  private Boolean enabled = false;
  private String remoteUrl = null;
  private UsernamePasswordCredentialsProvider cred = null;

  private List<String> mirroredPackages = null;

  private void initMirroredPackages() {
    mirroredPackages = new ArrayList<>();
  }

  private boolean isMirroredOtherwiseAddIt(final String packageName) {
    boolean isMirrored = mirroredPackages.contains(packageName);
    if (!isMirrored) {
      mirroredPackages.add(packageName);
    }
    return isMirrored;
  }


  public GitUtils(Configuration configuration) {
    Map<String, Object> gitSettings = (null != configuration && null != configuration.getAttributes())
        ? configuration.getAttributes().get("gitSettings") : null;
    if (null != gitSettings && (Boolean) gitSettings.get("enabled")) {
      this.enabled = (Boolean) gitSettings.get("enabled");
      this.remoteUrl = (String) gitSettings.get("remoteUrl");
      this.cred = new UsernamePasswordCredentialsProvider((String) gitSettings.get("username"), (String) gitSettings.get("password"));
      initMirroredPackages();
    }
  }

  public Boolean isEnabled() {
    return enabled;
  }

  public String buildNewUrl(final String packageName) {
    return remoteUrl + packageName.replace('/', '_') + ".git";
  }

  public void duplicateGit(final String packageName, final String url, final String newUrl) {
    if (isMirroredOtherwiseAddIt(packageName)) {
      return;
    }
    Git git = null;
    File localPath = null;
    try {
      localPath = this.getTemporaryDirectory();
      git = this.cloneRepo(url, localPath);
      this.forcePush(git, newUrl);
      log.info("Package {} mirrored from {} to {}", packageName, url, newUrl);

    } catch (IOException | GitAPIException e) {
      log.error("Unable to clone and push git package {}, from {}, skipping", packageName, url);
      log.error(e.getMessage());
    } finally {
      try {
        this.cleanup(git, localPath);
      } catch (IOException e) {
        log.error("Unable to cleanup for package {}", packageName);
      }
    }

  }

  private File getTemporaryDirectory() throws IOException {
    // let's get a temporary working directory
    File localPath = File.createTempFile("JGitTestRepository", "");
    Files.delete(localPath.toPath());
    return localPath;
  }

  private Git cloneRepo(final String url, File localPath) throws GitAPIException {
    // git clone [url]
    return Git.cloneRepository()
        .setURI(url)
        .setDirectory(localPath)
        .call();
  }

  private void forcePush(Git git, final String newUrl) throws GitAPIException {
    // git push -f --set-upstream [newUrl]
    git.push()
        .setRemote(newUrl)
        .setForce(true)
        .setCredentialsProvider(cred)
        .call();
  }

  private void cleanup(Git git, File localPath) throws IOException {
    if (git != null) {
      git.close();
    }
    if (localPath != null) {
      Files.walk(localPath.toPath())
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

}
