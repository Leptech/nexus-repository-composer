package org.sonatype.nexus.repository.composer.rest.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.composer.rest.api.model.ComposerProxyRepositoryApiRequest;
import org.sonatype.nexus.repository.composer.rest.api.model.GitProxyAttributes;
import org.sonatype.nexus.repository.composer.rest.api.model.GitSettingsAttributes;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import static java.util.Objects.nonNull;

/**
 *
 */
@Named
public class ComposerProxyRepositoryApiRequestToConfigurationConverter<T extends ComposerProxyRepositoryApiRequest>
    extends ProxyRepositoryApiRequestToConfigurationConverter<T> {

  @Inject
  public ComposerProxyRepositoryApiRequestToConfigurationConverter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  public Configuration convert(final T request) {
    Configuration configuration = super.convert(request);
    convertGitSettings(request, configuration);
    return configuration;
  }


  private void convertGitSettings(final T request, final Configuration configuration) {
    GitSettingsAttributes gitSettings = request.getGitSettings();
    if (nonNull(gitSettings)) {
      NestedAttributesMap gitSettingsConfiguration = configuration.attributes("gitSettings");
      gitSettingsConfiguration.set("enabled", gitSettings.getEnabled());
      gitSettingsConfiguration.set("remoteUrl", gitSettings.getRemoteUrl());
      gitSettingsConfiguration.set("username", gitSettings.getUsername());
      gitSettingsConfiguration.set("password", gitSettings.getPassword());
      convertProxy(gitSettings, gitSettingsConfiguration);
    }
  }

  private void convertProxy(
      final GitSettingsAttributes gitSettings,
      final NestedAttributesMap gitSettingsConfiguration) {
    GitProxyAttributes proxy = gitSettings.getGitProxyAttributes();
    if (nonNull(proxy)) {
      NestedAttributesMap proxyConfiguration = gitSettingsConfiguration.child("proxy");
      proxyConfiguration.set("httpHost", proxy.getHttpHost());
      proxyConfiguration.set("httpPort", proxy.getHttpPort());
      proxyConfiguration.set("httpUsername", proxy.getHttpUsername());
      proxyConfiguration.set("httpPassword", proxy.getHttpPassword());
      proxyConfiguration.set("httpsHost", proxy.getHttpsHost());
      proxyConfiguration.set("httpsPort", proxy.getHttpsPort());
      proxyConfiguration.set("httpsUsername", proxy.getHttpsUsername());
      proxyConfiguration.set("httpsPassword", proxy.getHttpsPassword());
      proxyConfiguration.set("nonProxyHosts", proxy.getNonProxyHosts());
    }
  }

}
