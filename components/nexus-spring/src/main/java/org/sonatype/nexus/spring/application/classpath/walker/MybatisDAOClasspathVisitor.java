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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MybatisDAOClasspathVisitor
    extends AbstractClassnameIndexingClasspathVisitor
    implements ClasspathVisitor
{
  private static final Logger LOG = LoggerFactory.getLogger(MybatisDAOClasspathVisitor.class);

  private final List<String> daoComponents = new ArrayList<>();

  @Override
  public String name() {
    return "MyBatis DAO Classpath Visitor";
  }

  @Override
  protected void doVisit(
      final String path,
      final String applicationJarPath,
      final InputStream applicationJarInputStream)
  {
    daoComponents.add(path);
    LOG.debug("Adding DAO class to cache: {}", path);
  }

  @Override
  protected boolean applies(final String path) {
    return path.endsWith("DAO.class");
  }

  @Override
  protected String getCacheFileName() {
    return "mybatis/daos.index";
  }

  @Override
  protected List<String> getCachedFileContent() {
    return daoComponents;
  }
}
