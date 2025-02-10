/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.spring.application.classpath.walker;

import java.util.Set;

import org.sonatype.nexus.spring.application.classpath.components.SisuComponentMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class SisuAggregatedIndexClasspathVisitorTest
    extends AbstractClasspathVisitorTest<SisuAggregatedIndexClasspathVisitor>
{
  private SisuComponentMap sisuComponentMap;

  @Override
  protected SisuAggregatedIndexClasspathVisitor newInstance() {
    sisuComponentMap = new SisuComponentMap();
    return new SisuAggregatedIndexClasspathVisitor(sisuComponentMap);
  }

  @Override
  protected String getAllNestedComponentsJarName() {
    return "sisu-all-components.jar";
  }

  @Override
  protected String getSomeNestedComponentsJarName() {
    return "sisu-some-components.jar";
  }

  @Override
  protected String getNoNestedComponentsJarName() {
    return "sisu-no-components.jar";
  }

  @Override
  protected void assertAllNestedComponentsAggregatedIndex() {
    Set<String> components = sisuComponentMap.getComponents("BOOT-INF/lib/nested-test1.jar");
    assertThat(components.size(), is(120));
    assertThat(components, hasItem("org.sonatype.nexus.internal.analytics.AnalyticsSecurityContributor"));
    assertThat(components, hasItem("org.sonatype.nexus.utils.httpclient.UserAgentGenerator"));
    components = sisuComponentMap.getComponents("BOOT-INF/lib/nested-test2.jar");
    assertThat(components.size(), is(9));
    assertThat(components, hasItem("org.sonatype.nexus.crypto.internal.CryptoHelperImpl"));
    assertThat(components, hasItem("org.sonatype.nexus.crypto.secrets.internal.SecretsServiceImpl"));
    assertThat(sisuComponentMap.getComponents().size(), is(129));
  }

  @Override
  protected void assertSomeNestedComponentsAggregatedIndex() {
    Set<String> components = sisuComponentMap.getComponents("BOOT-INF/lib/nested-test2.jar");
    assertThat(components.size(), is(9));
    assertThat(components, hasItem("org.sonatype.nexus.crypto.internal.CryptoHelperImpl"));
    assertThat(components, hasItem("org.sonatype.nexus.crypto.secrets.internal.SecretsServiceImpl"));
    assertThat(sisuComponentMap.getComponents().size(), is(9));
  }

  @Override
  protected void assertNoNestedComponentsAggregatedIndex() {
    Set<String> components = sisuComponentMap.getComponents();
    assertThat(components.size(), is(0));
  }
}
