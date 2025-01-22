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
package org.sonatype.nexus.internal.capability.upgrade;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuditAndQuarantineCapabilityDisableTask}
 */
public class AuditAndQuarantineCapabilityDisableTaskTest
    extends TestSupport
{
  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private CapabilityReference capabilityReference;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  @Mock
  private CapabilityContext context;

  @Mock
  private CapabilityType capabilityType1;

  @Mock
  private CapabilityType capabilityType2;

  private AuditAndQuarantineCapabilityDisableTask underTest;

  @Before
  public void setUp() {
    underTest = new AuditAndQuarantineCapabilityDisableTask(capabilityRegistry, repositoryManager);
    Collection<CapabilityReference> capabilityReferences = Arrays.asList(capabilityReference);
    when(capabilityRegistry.getAll()).thenReturn((Collection) capabilityReferences);
    when(capabilityType1.toString()).thenReturn("firewall.audit");
    when(context.type()).thenReturn(capabilityType1);
    when(capabilityReference.context()).thenReturn(context);
    when(capabilityReference.context().isEnabled()).thenReturn(true);

    Map<String, String> properties = new HashMap<>();
    properties.put("repository", "test-repo");
    when(capabilityReference.context().properties()).thenReturn(properties);

    when(repositoryManager.get("test-repo")).thenReturn(repository);
    when(format.getValue()).thenReturn("unsupported-format-1");
    when(repository.getFormat()).thenReturn(format);
  }

  @Test
  public void testExecute_disablesUnsupportedFormatCapability() throws Exception {
    underTest.execute();
    verify(capabilityRegistry).disable(capabilityReference.context().id());
  }

  @Test
  public void testExecute_doesNotDisableNonEnabledCapability() throws Exception {
    when(capabilityReference.context().isEnabled()).thenReturn(false);

    underTest.execute();
    verify(capabilityRegistry, never()).disable(capabilityReference.context().id());
  }

  @Test
  public void testExecute_doesNotDisableSupportedFormatCapability() throws Exception {
    when(format.getValue()).thenReturn("maven2");
    underTest.execute();
    verify(capabilityRegistry, never()).disable(capabilityReference.context().id());
  }

  @Test
  public void testExecute_doesNotDisableNonFirewallAuditCapability() throws Exception {
    when(capabilityType2.toString()).thenReturn("audit");
    when(context.type()).thenReturn(capabilityType2);

    underTest.execute();
    verify(capabilityRegistry, never()).disable(capabilityReference.context().id());
  }
}
