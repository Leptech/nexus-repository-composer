package org.sonatype.nexus.repository.composer.rest.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.sonatype.nexus.validation.constraint.UriString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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


  @JsonCreator
  public GitSettingsAttributes(
      @JsonProperty("enabled") final Boolean enabled,
      @JsonProperty("remoteUrl") final String remoteUrl,
      @JsonProperty("username") final String username,
      @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY) final String password) {
    this.enabled = enabled;
    this.remoteUrl = remoteUrl;
    this.username = username;
    this.password = password;
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
}
