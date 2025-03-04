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
package org.sonatype.nexus.upgrade.datastore.internal.steps;

import java.sql.Connection;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

@Named
@Singleton
public class RemoveDistributedCooperationMigrationStep_2_14
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS cooperation_lock";

  @Override
  public Optional<String> version() {
    return Optional.of("2.14");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    log.info("Dropping unused table cooperation_lock");
    runStatement(connection, DROP_TABLE_SQL);
  }
}
