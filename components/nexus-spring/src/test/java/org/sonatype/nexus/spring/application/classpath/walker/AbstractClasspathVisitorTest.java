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

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class AbstractClasspathVisitorTest<T extends ClasspathVisitor>
{
  private static final String CACHE_FILE_BASE =
      "target/test-classes/org/sonatype/nexus/spring/application/classpath/walker/";

  private T underTest;

  private File dataDir;

  private File aggregatedIndex;

  private ApplicationJarFilter applicationJarFilter = new AllowAllApplicationJarFilter();

  @Before
  public void setup() {
    dataDir = new File("target");
    underTest = newInstance();
    underTest.init(dataDir);
    aggregatedIndex = new File(dataDir, getCacheFileName());
    if (aggregatedIndex.exists()) {
      aggregatedIndex.delete();
    }
  }

  @Test
  public void testCacheAggregatedIndex_allNestedComponentJars() throws Exception {
    File testJar = new File(CACHE_FILE_BASE + getAllNestedComponentsJarName());
    new ClasspathWalker(singletonList(underTest), applicationJarFilter).walk(testJar, dataDir);

    File aggregatedIndex = new File(dataDir, getCacheFileName());
    assertThat(aggregatedIndex.exists(), is(true));
    List<String> lines = Files.readAllLines(aggregatedIndex.toPath());
    assertAllNestedComponentsAggregatedIndex(lines);
  }

  @Test
  public void testCacheAggregatedIndex_someNestedComponentJars() throws Exception {
    File testJar = new File(CACHE_FILE_BASE + getSomeNestedComponentsJarName());
    new ClasspathWalker(singletonList(underTest), applicationJarFilter).walk(testJar, dataDir);

    File aggregatedIndex = new File(dataDir, getCacheFileName());
    assertThat(aggregatedIndex.exists(), is(true));
    List<String> lines = Files.readAllLines(aggregatedIndex.toPath());
    assertSomeNestedComponentsAggregatedIndex(lines);
  }

  @Test
  public void testCacheAggregatedIndex_noNestedComponentJars() throws Exception {
    File testJar = new File(CACHE_FILE_BASE + getNoNestedComponentsJarName());
    new ClasspathWalker(singletonList(underTest), applicationJarFilter).walk(testJar, dataDir);

    File aggregatedIndex = new File(dataDir, getCacheFileName());
    assertThat(aggregatedIndex.exists(), is(false));
  }

  protected abstract T newInstance();

  protected abstract String getCacheFileName();

  protected abstract String getAllNestedComponentsJarName();

  protected abstract String getSomeNestedComponentsJarName();

  protected abstract String getNoNestedComponentsJarName();

  protected abstract void assertAllNestedComponentsAggregatedIndex(final List<String> lines);

  protected abstract void assertSomeNestedComponentsAggregatedIndex(final List<String> lines);
}
