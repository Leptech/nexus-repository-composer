package org.sonatype.nexus.repository.composer.rest.api.model;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.validation.constraint.UriString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * REST API model describing a gitSettings repository.
 */
public class GitSettingsAttributes {
  @ApiModelProperty(value = "Is git mirroring activated ?", example = "true")
  @NotNull
  protected final Boolean enabled;

  @ApiModelProperty(value = "Location of the remote git", example = "https://gitlab.com/maxdestors/")
  @UriString
  @NotEmpty
  protected final String remoteUrl;

  @ApiModelProperty
  protected final String username;

  @ApiModelProperty(access = "writeOnly")
  protected final String password;

  @NotNull
  @Valid
  private final GitProxyAttributes gitProxyAttributes;


  @JsonCreator
  public GitSettingsAttributes(
      @JsonProperty("enabled") final Boolean enabled,
      @JsonProperty("remoteUrl") final String remoteUrl,
      @JsonProperty("username") final String username,
      @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY) final String password,
      @JsonProperty("proxy") final GitProxyAttributes gitProxyAttributes
  ) {
    this.enabled = enabled;
    this.remoteUrl = remoteUrl;
    this.username = username;
    this.password = password;
    this.gitProxyAttributes = gitProxyAttributes;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public String getRemoteUrl() {
    return remoteUrl;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public GitProxyAttributes getGitProxyAttributes() {
    return gitProxyAttributes;
  }
}
