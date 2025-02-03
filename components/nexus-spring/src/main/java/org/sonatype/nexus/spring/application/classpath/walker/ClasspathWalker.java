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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Named
@Singleton
public class ClasspathWalker
{
  private static final Logger LOG = LoggerFactory.getLogger(ClasspathWalker.class);

  private final List<ClasspathVisitor> classpathVisitors;

  private final ApplicationJarFilter applicationJarFilter;

  @Inject
  public ClasspathWalker(
      final List<ClasspathVisitor> classpathVisitors,
      final ApplicationJarFilter applicationJarFilter)
  {
    this.classpathVisitors = classpathVisitors;
    this.applicationJarFilter = applicationJarFilter;
  }

  public void walk(final File base, final File dataDirectory) throws Exception {
    // Visitors MUST be initialized prior to ANY other usages
    for (ClasspathVisitor classpathVisitor : classpathVisitors) {
      classpathVisitor.init(dataDirectory);
    }

    // If all visitors have cache available, skip the classpath walk, save a LOT of time
    if (isFullVisitorCacheAvailable()) {
      LOG.debug("Skipping classpath walk as all visitors have cache available");
      return;
    }

    LOG.debug("Building the IoC classpath index(es) from: {}", base);
    List<String> applicationJarPaths = getApplicationJarPaths(base);

    for (String applicationJarPath : applicationJarPaths) {
      LOG.debug("Walking classpath of: {} from base {}", applicationJarPath, base);
      if (base.isFile()) {
        try (JarFile baseJar = new JarFile(base)) {
          visitJarEntries(applicationJarPath, baseJar);
        }
        for (ClasspathVisitor classpathVisitor : classpathVisitors) {
          classpathVisitor.done();
        }
      }
    }
  }

  private void visitJarEntries(String applicationJarPath, JarFile baseJar) throws IOException {
    JarEntry nestedJar = baseJar.getJarEntry(applicationJarPath);
    try (JarInputStream applicationJarInputStream = new JarInputStream(baseJar.getInputStream(nestedJar))) {
      JarEntry nestedJarEntry = applicationJarInputStream.getNextJarEntry();
      while (nestedJarEntry != null) {
        LOG.debug("Visiting entry: {} in {}", nestedJarEntry.getName(), applicationJarPath);
        for (ClasspathVisitor classpathVisitor : classpathVisitors) {
          // with single inputstream only one visitor can successfully visit
          if (classpathVisitor.visit(nestedJarEntry.getName(), applicationJarPath, applicationJarInputStream)) {
            break;
          }
        }
        nestedJarEntry = applicationJarInputStream.getNextJarEntry();
      }
    }
  }

  private boolean isFullVisitorCacheAvailable() {
    for (ClasspathVisitor classpathVisitor : classpathVisitors) {
      if (classpathVisitor.needToVisit()) {
        LOG.debug("Classpath visitor needs to rebuild its cache {}", classpathVisitor.name());
        return false;
      }
    }
    return true;
  }

  private List<String> getApplicationJarPaths(final File base) throws IOException {
    String springBootClasspathIndexPath = "BOOT-INF/classpath.idx";
    if (base.isFile()) {
      try (JarFile jarFile = new JarFile(base)) {
        JarEntry indexEntry = jarFile.getJarEntry(springBootClasspathIndexPath);
        if (indexEntry != null) {
          return filterForSupportedApplications(fileToStringList(jarFile.getInputStream(indexEntry)));
        }
      }
    }
    else if (base.isDirectory()) {
      File indexFile = new File(base, springBootClasspathIndexPath);
      if (indexFile.exists() && indexFile.isFile()) {
        return filterForSupportedApplications(fileToStringList(Files.newInputStream(indexFile.toPath())));
      }
    }
    return emptyList();
  }

  private List<String> filterForSupportedApplications(final List<String> applicationJarPaths) {
    return applicationJarPaths.stream().filter(applicationJarFilter::allowed).collect(toList());
  }

  private List<String> fileToStringList(final InputStream inputStream) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      return reader
          .lines()
          .filter(line -> !line.trim().isEmpty())
          .map(line -> line.trim().replace("- ", "").replace("\"", ""))
          .collect(toList());
    }
  }
}
