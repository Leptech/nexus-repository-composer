/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.composer.internal;

import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GitUtilsTest
    extends TestSupport {


  @Test
  public void gitMirroringDisabled() {
    GitUtils git = new GitUtils(buildConfig(false));
    assertFalse(git.isEnabled());
  }

  @Test
  public void gitMirroringEnabled() {
    String packageName = "jeremykenedy/laravel-roles";
    String url = "https://github.com/jeremykenedy/laravel-roles.git";

    GitUtils git = new GitUtils(buildConfig(true));

    assertTrue(git.isEnabled());

    String newUrl = git.buildNewUrl(packageName);

    assertNotNull(newUrl);

    // TODO
    // git.duplicateGit(packageName, url, newUrl);
  }


  private ConfigurationData buildConfig(boolean enabled) {
    ConfigurationData configuration = new ConfigurationData();
    NestedAttributesMap gitSettingsConfiguration = configuration.attributes("gitSettings");
    gitSettingsConfiguration.set("enabled", enabled);
    if (enabled) {
      gitSettingsConfiguration.set("remoteUrl", "https://gitlab.com/myUsername/");
      gitSettingsConfiguration.set("username", "myUsername");
      gitSettingsConfiguration.set("password", "somePassword");
    }
    return configuration;
  }

}
