package org.sonatype.nexus.repository.composer.rest.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * REST API model describing a gitSettings repository.
 */
public class GitProxyAttributes {

  @ApiModelProperty
  protected final String httpHost;

  @ApiModelProperty
  protected final Integer httpPort;

  @ApiModelProperty
  protected final String httpUsername;

  @ApiModelProperty(access = "writeOnly")
  protected final String httpPassword;

  @ApiModelProperty
  protected final String httpsHost;

  @ApiModelProperty
  protected final Integer httpsPort;

  @ApiModelProperty
  protected final String httpsUsername;

  @ApiModelProperty(access = "writeOnly")
  protected final String httpsPassword;

  @JsonCreator
  public GitProxyAttributes(
      @JsonProperty("httpHost") final String httpHost,
      @JsonProperty("httpPort") final Integer httpPort,
      @JsonProperty("httpUsername") final String httpUsername,
      @JsonProperty(value = "httpPassword", access = JsonProperty.Access.WRITE_ONLY) final String httpPassword,
      @JsonProperty("httpsHost") final String httpsHost,
      @JsonProperty("httpsPort") final Integer httpsPort,
      @JsonProperty("httpsUsername") final String httpsUsername,
      @JsonProperty(value = "httpsPassword", access = JsonProperty.Access.WRITE_ONLY) final String httpsPassword
  ) {
    this.httpHost = httpHost;
    this.httpPort = httpPort;
    this.httpUsername = httpUsername;
    this.httpPassword = httpPassword;
    this.httpsHost = httpsHost;
    this.httpsPort = httpsPort;
    this.httpsUsername = httpsUsername;
    this.httpsPassword = httpsPassword;
  }

  public String getHttpHost() {
    return httpHost;
  }

  public Integer getHttpPort() {
    return httpPort;
  }

  public String getHttpUsername() {
    return httpUsername;
  }

  public String getHttpPassword() {
    return httpPassword;
  }

  public String getHttpsHost() {
    return httpsHost;
  }

  public Integer getHttpsPort() {
    return httpsPort;
  }

  public String getHttpsUsername() {
    return httpsUsername;
  }

  public String getHttpsPassword() {
    return httpsPassword;
  }
}
