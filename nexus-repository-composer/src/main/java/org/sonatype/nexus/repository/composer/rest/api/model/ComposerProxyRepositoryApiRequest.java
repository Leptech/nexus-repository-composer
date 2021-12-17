package org.sonatype.nexus.repository.composer.rest.api.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

/**
 *
 */
public class ComposerProxyRepositoryApiRequest
        extends ProxyRepositoryApiRequest
{

    @NotNull
    @Valid
    private final GitSettingsAttributes gitSettings;

    @SuppressWarnings("squid:S00107") // suppress constructor parameter count
    @JsonCreator
    public ComposerProxyRepositoryApiRequest(
            @JsonProperty("name") final String name,
            @JsonProperty("format") final String format,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final StorageAttributes storage,
            @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
            @JsonProperty("proxy") final ProxyAttributes proxy,
            @JsonProperty("negativeCache") final NegativeCacheAttributes negativeCache,
            @JsonProperty("httpClient") final HttpClientAttributes httpClient,
            @JsonProperty("routingRule") final String routingRule,
            @JsonProperty("gitSettings") final GitSettingsAttributes gitSettings)
    {
        super(name, format, online, storage, cleanup, proxy, negativeCache, httpClient, routingRule);
        this.gitSettings = gitSettings;
    }


    public GitSettingsAttributes getGitSettings() {
        return gitSettings;
    }

}
