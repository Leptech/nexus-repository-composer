package org.sonatype.nexus.repository.composer.internal;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.config.Configuration;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;


public class GitMirroringUtils {
  protected final Logger log = Preconditions.checkNotNull(Loggers.getLogger(this));
  private Pattern nonProxyHostPattern = null;
  private Boolean enabled = false;
  private String remoteUrl = null;
  private UsernamePasswordCredentialsProvider cred = null;
  private List<String> processedPackages = null;

  public GitMirroringUtils(Configuration configuration) {
    NestedAttributesMap gitSettings = null != configuration ? configuration.attributes("gitSettings") : null;
    if (null != gitSettings) {
      Boolean enabled = gitSettings.get("enabled", Boolean.class);
      if (null != enabled && enabled) {
        this.enabled = true;
        this.remoteUrl = gitSettings.get("remoteUrl", String.class);
        this.cred = new UsernamePasswordCredentialsProvider(
            gitSettings.get("username", String.class),
            nonNullString(gitSettings.get("password", String.class)));
        initProcessedPackages();
        if (gitSettings.contains("proxy")) {
          NestedAttributesMap proxy = gitSettings.child("proxy");
          initProxySelector(proxy.get("httpHost", String.class),
              toInt(proxy.get("httpPort", Number.class)),
              proxy.get("httpUsername", String.class),
              proxy.get("httpPassword", String.class),
              proxy.get("httpsHost", String.class),
              toInt(proxy.get("httpsPort", Number.class)),
              proxy.get("httpsUsername", String.class),
              proxy.get("httpsPassword", String.class));
          this.nonProxyHostPattern = buildNonProxyHostPattern(proxy.get("nonProxyHosts", new TypeToken<List<String>>() {
          }));
        }
      }
    }
  }

  protected static Integer toInt(final Number num) {
    return num == null ? null : num.intValue();
  }

  protected static String nonNullString(final String str) {
    return str == null ? "" : str;
  }

  private void initProcessedPackages() {
    processedPackages = new ArrayList<>();
  }

  private boolean isProcessedOtherwiseAddIt(final String packageName) {
    boolean isProcessed = processedPackages.contains(packageName);
    if (!isProcessed) {
      processedPackages.add(packageName);
    }
    return isProcessed;
  }

  private void initProxySelector(
      String httpHost, Integer httpPort, String httpUsername, String httpPassword,
      String httpsHost, Integer httpsPort, String httpsUsername, String httpsPassword) {

    ProxySelector.setDefault(new ProxySelector() {
      final ProxySelector delegate = ProxySelector.getDefault();

      @Override
      public List<Proxy> select(URI uri) {
        // Filter the URIs to be proxied
        if (!noProxyFor(uri.getHost())) {
          if (uri.toString().startsWith("https")
              && null != httpsHost && null != httpsPort) {
            return Arrays.asList(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(httpsHost, httpsPort)));
          }
          if (uri.toString().startsWith("http")
              && null != httpHost && null != httpPort) {
            return Arrays.asList(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(httpHost, httpPort)));
          }
        }
        // revert to the default behaviour
        return delegate == null ? Arrays.asList(Proxy.NO_PROXY) : delegate.select(uri);
      }

      @Override
      public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (uri == null || sa == null || ioe == null) {
          throw new IllegalArgumentException("Arguments can't be null.");
        }
      }
    });
    if ((null != httpUsername && null != httpPassword)
        || (null != httpsUsername && null != httpsPassword)) {

      Authenticator.setDefault(new Authenticator() {

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
          String username = "";
          String password = "";
          if ("https".equals(getRequestingProtocol()) && null != httpsUsername && null != httpsPassword) {
            username = httpsUsername;
            password = httpsPassword;
          } else if ("http".equals(getRequestingProtocol()) && null != httpUsername && null != httpPassword) {
            username = httpUsername;
            password = httpPassword;
          }
          return new PasswordAuthentication(username, password.toCharArray());
        }
      });
    }
    if (null != httpUsername && null != httpPassword) {
      System.setProperty("http.proxyUser", httpUsername);
      System.setProperty("http.proxyPassword", httpPassword);
      System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }
    if (null != httpsUsername && null != httpsPassword) {
      System.setProperty("https.proxyUser", httpsUsername);
      System.setProperty("https.proxyPassword", httpsPassword);
      System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }
  }

  public Boolean isEnabled() {
    return enabled;
  }

  public String buildNewUrl(final String packageName) {
    return remoteUrl + packageName.replace('/', '_') + ".git";
  }

  public void duplicate(final String packageName, final String url, final String newUrl) {
    if (isProcessedOtherwiseAddIt(packageName)) {
      return;
    }
    Git git = null;
    File localPath = null;
    try {
      localPath = this.getTemporaryDirectory();
      git = this.cloneRepo(url, localPath);
      log.info("Package {} cloned from {}", packageName, url);
      this.forcePush(git, newUrl);
      log.info("Package {} mirrored to {}", packageName, newUrl);

    } catch (IOException | GitAPIException e) {
      log.error("Unable to clone or push git package {}, from {}, skipping", packageName, url);
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
    File localPath = File.createTempFile("TmpRepo", "");
    Files.delete(localPath.toPath());
    return localPath;
  }

  private Git cloneRepo(final String url, File localPath) throws GitAPIException {
    // git clone [url]
    return Git.cloneRepository()
        .setURI(url)
        .setDirectory(localPath)
        .setBare(true)
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


  private Pattern buildNonProxyHostPattern(final List<String> nonProxyHosts) {
    if (null == nonProxyHosts) {
      return null;
    }
    LinkedHashSet<String> patterns = new LinkedHashSet<>(nonProxyHosts);
    String nonProxyPatternString = Joiner.on("|").join(patterns.stream().map(
        input -> "(" + input.toLowerCase(Locale.US).replaceAll("\\.", "\\\\.")
            .replaceAll("\\*", ".*?").replaceAll("\\[", "\\\\[")
            .replaceAll("\\]", "\\\\]") + ")"
    ).collect(Collectors.toList()));
    Pattern nonProxyPattern = null;
    if (!Strings2.isBlank(nonProxyPatternString)) {
      try {
        nonProxyPattern = Pattern.compile(nonProxyPatternString, Pattern.CASE_INSENSITIVE);
      } catch (PatternSyntaxException e) {
        log.warn("Invalid non-proxy host regex: {}, using defaults", nonProxyPatternString, e);
      }
    }
    return nonProxyPattern;
  }

  private boolean noProxyFor(final String hostName) {
    return nonProxyHostPattern != null && nonProxyHostPattern.matcher(hostName).matches();
  }
}
