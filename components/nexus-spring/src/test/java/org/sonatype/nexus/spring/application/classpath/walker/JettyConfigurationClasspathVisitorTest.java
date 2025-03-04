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

public class JettyConfigurationClasspathVisitorTest
    extends AbstractClasspathVisitorTest<JettyConfigurationClasspathVisitor>
{
  @Override
  protected JettyConfigurationClasspathVisitor newInstance() {
    return new JettyConfigurationClasspathVisitor();
  }

  @Override
  protected String getCacheFileName() {
    return "cache/jetty/configurations.index";
  }

  @Override
  protected String getAllNestedComponentsJarName() {
    return "jetty-all-components.jar";
  }

  @Override
  protected String getSomeNestedComponentsJarName() {
    return "jetty-some-components.jar";
  }

  @Override
  protected String getNoNestedComponentsJarName() {
    return "jetty-no-components.jar";
  }

  @Override
  protected void assertSomeNestedComponentsAggregatedIndex(final List<String> lines) {
    assertThat(lines.size(), is(2));
    assertThat(lines.get(0), is("org/sonatype/nexus/jetty/AnotherTest1ConnectorConfiguration.class"));
    assertThat(lines.get(1), is("org/sonatype/nexus/jetty/AnotherTest2ConnectorConfiguration.class"));
  }

  @Override
  protected void assertAllNestedComponentsAggregatedIndex(final List<String> lines) {
    assertThat(lines.size(), is(4));
    assertThat(lines.get(0), is("org/sonatype/nexus/jetty/Test1ConnectorConfiguration.class"));
    assertThat(lines.get(1), is("org/sonatype/nexus/jetty/Test2ConnectorConfiguration.class"));
    assertThat(lines.get(2), is("org/sonatype/nexus/jetty/AnotherTest1ConnectorConfiguration.class"));
    assertThat(lines.get(3), is("org/sonatype/nexus/jetty/AnotherTest2ConnectorConfiguration.class"));
  }
}
