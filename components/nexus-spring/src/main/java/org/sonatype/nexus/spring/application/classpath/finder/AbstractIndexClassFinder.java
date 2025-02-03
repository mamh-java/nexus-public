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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.sisu.space.ClassFinder;
import org.eclipse.sisu.space.ClassSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.asEnumeration;

public abstract class AbstractIndexClassFinder
    implements ClassFinder
{
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractIndexClassFinder.class);

  private final File indexCache;

  private final List<ClassFinderFilter> classFinderFilters;

  public AbstractIndexClassFinder(
      final File indexCacheDirectory,
      final String indexCachePath,
      final List<ClassFinderFilter> classFinderFilters)
  {
    this.indexCache = new File(indexCacheDirectory, indexCachePath);
    if (!indexCache.exists()) {
      throw new IllegalStateException("index cache does not exist: " + indexCache);
    }
    this.classFinderFilters = checkNotNull(classFinderFilters);
  }

  @Override
  public Enumeration<URL> findClasses(final ClassSpace space) {
    try {
      List<String> lines = Files.readAllLines(indexCache.toPath());
      LOG.debug("Found {} entries in index cache {}", lines.size(), indexCache);
      List<URL> urls = new ArrayList<>();
      for (String line : lines) {
        line = postProcessLine(line);
        if (line.startsWith("-")) {
          continue;
        }
        // check if the line is allowed by any of the filters, only takes 1 to allow
        for (ClassFinderFilter classFinderFilter : classFinderFilters) {
          if (classFinderFilter.allowed(line)) {
            URL url = space.getResource(line);
            LOG.debug("Resolved {} to {}", line, url);
            if (url != null) {
              urls.add(url);
            }
            break;
          }
        }
      }
      return asEnumeration(urls.iterator());
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to read index cache " + indexCache, e);
    }
  }

  protected String postProcessLine(final String line) {
    // no-op by default
    return line;
  }
}
