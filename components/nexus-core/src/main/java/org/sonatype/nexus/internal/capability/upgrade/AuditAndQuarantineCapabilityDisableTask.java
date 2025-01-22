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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.firewall.FirewallCapability;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.capability.firewall.FirewallCapability.AUDIT_QUARANTINE_CAPABILITY_ID;

@Named
public class AuditAndQuarantineCapabilityDisableTask
    extends TaskSupport
{
  static final String MESSAGE =
      "Upgrade - disable audit and quarantine capability for repository with unsupported format";

  private final CapabilityRegistry capabilityRegistry;

  private final RepositoryManager repositoryManager;

  @Inject
  public AuditAndQuarantineCapabilityDisableTask(
      final CapabilityRegistry capabilityRegistry,
      final RepositoryManager repositoryManager)
  {
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  protected Object execute() throws Exception {
    capabilityRegistry.getAll()
        .stream()
        .filter(this::isFirewallAuditCapabilityEnabledAndFormatNotSupported)
        .forEach(this::disableCapability);
    return null;
  }

  @Override
  public String getMessage() {
    return MESSAGE;
  }

  private boolean isFirewallAuditCapabilityEnabledAndFormatNotSupported(CapabilityReference registry) {
    boolean isFirewallAuditCapabilityType = registry.context().type().toString().equals(AUDIT_QUARANTINE_CAPABILITY_ID);
    boolean isFirewallAuditCapabilityEnabled = registry.context().isEnabled();
    String repositoryName = registry.context().properties().get("repository");

    if (repositoryName == null) {
      return false;
    }

    Repository repository = repositoryManager.get(repositoryName);
    String format = repository.getFormat().getValue();
    boolean isFirewallSupportedFormat = FirewallCapability.isFirewallSupportedFormat(format);

    return isFirewallAuditCapabilityType && isFirewallAuditCapabilityEnabled && !isFirewallSupportedFormat;
  }

  private void disableCapability(CapabilityReference registry) {
    log.info("Disabling audit and quarantine capability for {} with unsupported format",
        registry.context().properties().get("repository"));
    capabilityRegistry.disable(registry.context().id());
  }
}
