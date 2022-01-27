package org.sonatype.nexus.repository.composer.internal;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.config.Configuration;

import com.google.common.base.Joiner;
import com.google.common.reflect.TypeToken;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


public class GitMirroringUtils {
  protected final Logger log = checkNotNull(Loggers.getLogger(this));

  private Boolean enabled = false;
  private String remoteUrl = null;
  private String groupName = null;
  private UsernamePasswordCredentialsProvider cred = null;
  private Pattern nonProxyHostPattern = null;
  private String apiToken = null;

  private List<String> processedPackages = null;

  public GitMirroringUtils(Configuration configuration) {
    NestedAttributesMap gitSettings = isNull(configuration) ? null : configuration.attributes("gitSettings");
    if (null != gitSettings) {
      Boolean enabled = gitSettings.get("enabled", Boolean.class);
      if (null != enabled && enabled) {
        this.enabled = true;
        this.remoteUrl = gitSettings.get("remoteUrl", String.class);
        this.remoteUrl = isNull(this.remoteUrl) ? null : this.remoteUrl.replaceAll("/$", "");
        this.groupName = gitSettings.get("groupName", String.class);
        this.cred = new UsernamePasswordCredentialsProvider(
            gitSettings.get("username", String.class),
            nonNullString(gitSettings.get("password", String.class)));
        this.apiToken = gitSettings.get("apiToken", String.class);
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
    return isNull(num) ? null : num.intValue();
  }

  protected static String nonNullString(final String str) {
    return isNull(str) ? "" : str;
  }

  protected static String encode(final String str) throws UnsupportedEncodingException {
    return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
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
              && nonNull(httpsHost) && nonNull(httpsPort)) {
            return Arrays.asList(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(httpsHost, httpsPort)));
          }
          if (uri.toString().startsWith("http")
              && nonNull(httpHost) && nonNull(httpPort)) {
            return Arrays.asList(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(httpHost, httpPort)));
          }
        }
        // revert to the default behaviour
        return isNull(delegate) ? Arrays.asList(Proxy.NO_PROXY) : delegate.select(uri);
      }

      @Override
      public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (isNull(uri) || isNull(sa) || isNull(ioe)) {
          throw new IllegalArgumentException("Arguments can't be null.");
        }
      }
    });
    if ((nonNull(httpUsername) && nonNull(httpPassword))
        || (nonNull(httpsUsername) && nonNull(httpsPassword))) {

      Authenticator.setDefault(new Authenticator() {

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
          String username = "";
          String password = "";
          if ("https".equals(getRequestingProtocol()) && nonNull(httpsUsername) && nonNull(httpsPassword)) {
            username = httpsUsername;
            password = httpsPassword;
          } else if ("http".equals(getRequestingProtocol()) && nonNull(httpUsername) && nonNull(httpPassword)) {
            username = httpUsername;
            password = httpPassword;
          }
          return new PasswordAuthentication(username, password.toCharArray());
        }
      });
    }
    if (nonNull(httpUsername) && nonNull(httpPassword)) {
      System.setProperty("http.proxyUser", httpUsername);
      System.setProperty("http.proxyPassword", httpPassword);
      System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }
    if (nonNull(httpsUsername) && nonNull(httpsPassword)) {
      System.setProperty("https.proxyUser", httpsUsername);
      System.setProperty("https.proxyPassword", httpsPassword);
      System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }
  }

  public Boolean isEnabled() {
    return enabled;
  }

  public String buildNewUrl(final String packageName) {
    return remoteUrl + "/" + groupName + "/" + packageName.replace('/', '_') + ".git";
  }

  public void duplicate(final String packageName, final String url, final String newUrl) {
    if (isProcessedOtherwiseAddIt(packageName)) {
      return;
    }
    Executors.newSingleThreadExecutor().submit(() -> {
      duplicateProcess(packageName, url, newUrl);
    });
  }

  private void duplicateProcess(final String packageName, final String url, final String newUrl) {

    Git git = null;
    File localPath = null;
    try {
      localPath = getTemporaryDirectory();
      git = cloneRepo(url, localPath);
      log.info("Package {} cloned from {}", packageName, url);
      forcePush(git, newUrl);
      log.info("Package {} mirrored to {}", packageName, newUrl);
      if (nonNull(apiToken)) {
        makeRepositoryPublic(packageName);
        log.info("Repo is now public {}", newUrl);
      }

    } catch (IOException | GitAPIException e) {
      log.error("Unable to clone or push git package {}, from {}, skipping", packageName, url);
      log.error(e.getMessage());
    } finally {
      try {
        this.cleanup(git, localPath);
      } catch (IOException e) {
        log.error("Unable to cleanup for package {}, skipping", packageName);
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
    // git clone [url] --bare
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
    if (nonNull(git)) {
      git.close();
    }
    if (nonNull(localPath)) {
      Files.walk(localPath.toPath())
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  private void makeRepositoryPublic(String packageName) {
    try {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      HttpPut httpPut = new HttpPut(buildApiUrl(packageName));
      httpPut.setHeader("PRIVATE-TOKEN", apiToken);
      httpPut.setEntity(new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair("visibility", "public"))));

      try (CloseableHttpResponse response = httpclient.execute(httpPut)) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          log.warn("makeRepositoryPublic Response code : {}", statusCode);
        }
      }
    } catch (IOException e) {
      log.error("makeRepositoryPublic : {}", e.getMessage());
    }
  }

  private String buildApiUrl(final String packageName) throws UnsupportedEncodingException {
    String projectId = encode(this.groupName + "/" + packageName.replace('/', '_'));
    return remoteUrl + "/api/v4/projects/" + projectId;
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
    return nonNull(nonProxyHostPattern) && nonProxyHostPattern.matcher(hostName).matches();
  }
}
