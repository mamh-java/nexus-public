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
package org.sonatype.nexus.spring.application.classpath.finder;

import java.io.IOException;
import java.util.Set;

import org.sonatype.nexus.spring.application.NexusProperties;
import org.sonatype.nexus.spring.application.PropertyMap;
import org.sonatype.nexus.spring.application.classpath.components.FeatureFlagComponentMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeatureFlagEnabledClassFinderFilterTest
{
  private FeatureFlagComponentMap featureFlagComponentMap;

  @BeforeEach
  public void setup() {
    featureFlagComponentMap = mock(FeatureFlagComponentMap.class);
    when(featureFlagComponentMap.getComponents()).thenReturn(Set.of(
        "org.sonatype.nexus.test.MyFakeClassName/nexus.test/false/false"));
  }

  @Test
  public void testControl() throws IOException {
    NexusProperties nexusProperties = mock(NexusProperties.class);
    PropertyMap properties = new PropertyMap();
    when(nexusProperties.get()).thenReturn(properties);
    FeatureFlagEnabledClassFinderFilter filter =
        new FeatureFlagEnabledClassFinderFilter(featureFlagComponentMap, nexusProperties);

    assertThat(filter.allowed("org.sonatype.nexus.test.MyFakeClassName"), is(true));
  }
}
