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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SisuAggregatedIndexClasspathVisitorTest
    extends AbstractClasspathVisitorTest<SisuAggregatedIndexClasspathVisitor>
{
  @Override
  protected SisuAggregatedIndexClasspathVisitor newInstance() {
    return new SisuAggregatedIndexClasspathVisitor();
  }

  @Override
  protected String getCacheFileName() {
    return "cache/sisu/component.index";
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
  protected void assertAllNestedComponentsAggregatedIndex(final List<String> lines) {
    assertThat(lines.size(), is(131));
    assertThat(lines.get(0), is("- BOOT-INF/lib/nested-test1.jar"));
    // just validating first and last class, as opposed to every single line item
    assertThat(lines.get(1), is("org.sonatype.nexus.internal.analytics.AnalyticsSecurityContributor"));
    assertThat(lines.get(120), is("org.sonatype.nexus.utils.httpclient.UserAgentGenerator"));
    assertThat(lines.get(121), is("- BOOT-INF/lib/nested-test2.jar"));
    assertThat(lines.get(122), is("org.sonatype.nexus.crypto.internal.CryptoHelperImpl"));
    assertThat(lines.get(130), is("org.sonatype.nexus.crypto.secrets.internal.SecretsServiceImpl"));
  }

  @Override
  protected void assertSomeNestedComponentsAggregatedIndex(final List<String> lines) {
    assertThat(lines.size(), is(10));
    assertThat(lines.get(0), is("- BOOT-INF/lib/nested-test2.jar"));
    assertThat(lines.get(1), is("org.sonatype.nexus.crypto.internal.CryptoHelperImpl"));
    assertThat(lines.get(9), is("org.sonatype.nexus.crypto.secrets.internal.SecretsServiceImpl"));
  }
}
