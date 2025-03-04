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
package org.sonatype.nexus.repository.content.tasks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * This availability version is only set to 1.0 because the task is launched during upgrade process, before the db
 * migration version has been updated
 */
@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class CreateComponentIndexTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String NAME = "Repair - Recreate component indexes for performance";

  public static final String TYPE_ID = "create.component.index.task";

  public static final String EXPOSED_FLAG = "${nexus.component.index.task.exposed:-false}";

  public static final String VISIBLE_FLAG = "${nexus.component.index.task.visible:-false}";

  @Inject
  public CreateComponentIndexTaskDescriptor(
      @Named(EXPOSED_FLAG) final boolean exposed,
      @Named(VISIBLE_FLAG) final boolean visible)
  {
    super(TYPE_ID, CreateComponentIndexTask.class, NAME, visible, exposed);
  }
}
