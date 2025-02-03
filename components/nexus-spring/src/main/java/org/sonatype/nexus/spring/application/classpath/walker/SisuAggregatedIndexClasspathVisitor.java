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
public class SisuAggregatedIndexClasspathVisitor
    extends AbstractClassnameIndexingClasspathVisitor
    implements ClasspathVisitor
{
  private static final Logger LOG = LoggerFactory.getLogger(SisuAggregatedIndexClasspathVisitor.class);

  private final List<String> indexedComponents = new ArrayList<>();

  @Override
  public String name() {
    return "Sisu Aggregated Index Classpath Visitor";
  }

  @Override
  public void doVisit(
      final String path,
      final String applicationJarPath,
      final InputStream applicationJarInputStream)
  {
    List<String> components = toSimpleStringList(applicationJarPath, applicationJarInputStream);
    LOG.debug("Found indexed components: {}", components);
    indexedComponents.addAll(components);
  }

  @Override
  protected boolean applies(final String path) {
    return path.endsWith("META-INF/sisu/javax.inject.Named");
  }

  @Override
  protected String getCacheFileName() {
    return "sisu/component.index";
  }

  @Override
  protected List<String> getCachedFileContent() {
    return indexedComponents;
  }
}
