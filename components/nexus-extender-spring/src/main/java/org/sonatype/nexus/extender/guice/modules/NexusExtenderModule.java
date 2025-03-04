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
package org.sonatype.nexus.extender.guice.modules;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.extender.sisu.modules.SisuAggregatedIndexModule;
import org.sonatype.nexus.spring.application.NexusProperties;
import org.sonatype.nexus.spring.application.classpath.finder.NexusSisuAggregatedIndexClassFinder;
import org.sonatype.nexus.validation.ValidationModule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.apache.shiro.guice.aop.ShiroAopModule;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry module for sisu/guice configuration
 */
public class NexusExtenderModule
    extends AbstractModule
{
  private final URLClassSpace space;

  private final ShiroAopModule shiroAopModule = new ShiroAopModule();

  private final SecurityFilterModule securityFilterModule;

  private final MetricsRegistryModule metricsRegistryModule;

  private final ValidationModule validationModule = new ValidationModule();

  private final RankingModule rankingModule = new RankingModule();

  private final NexusProperties nexusProperties;

  private final NexusServletContextModule nexusServletContextModule;

  private static final Logger LOG = LoggerFactory.getLogger(NexusExtenderModule.class);

  public NexusExtenderModule(
      final NexusProperties nexusProperties,
      final ServletContext servletContext) throws IOException
  {
    this.space = new URLClassSpace(getClass().getClassLoader());
    this.nexusProperties = nexusProperties;
    this.nexusServletContextModule = new NexusServletContextModule(servletContext, nexusProperties.get());
    this.metricsRegistryModule = new MetricsRegistryModule(this.nexusProperties.get());
    this.securityFilterModule = new SecurityFilterModule(this.nexusProperties.get());
  }

  @Override
  protected void configure() {
    List<Module> modules = new ArrayList<>();
    URL[] allUrls = space.getURLs();
    LOG.debug("all classpath urls: {}", Arrays.toString(allUrls));
    try {
      Map<String, String> properties = nexusProperties.get();
      if (!properties.containsKey(BeanScanning.class.getName())) {
        properties.put(BeanScanning.class.getName(), BeanScanning.GLOBAL_INDEX.name());
      }

      // this classfinder will handle resolving what classes are available vs what are needed via feature flags
      NexusSisuAggregatedIndexClassFinder nexusSisuAggregatedIndexClassFinder =
          new NexusSisuAggregatedIndexClassFinder(new File(nexusProperties.get().get("karaf.data"), "cache"),
              nexusProperties);

      modules.add(new DataAccessModule(nexusProperties, space));
      modules.add(new SisuAggregatedIndexModule(space, nexusSisuAggregatedIndexClassFinder));
      modules.add(securityFilterModule);
      modules.add(nexusServletContextModule);
      modules.add(metricsRegistryModule);
      WebResourcesModule webResourcesModule = new WebResourcesModule(space);
      modules.add(webResourcesModule);
      modules.add(shiroAopModule);
      modules.add(validationModule);
      modules.add(rankingModule);

      bind(Logger.class).toInstance(LoggerFactory.getLogger(LogManager.class));

      new WireModule(modules).configure(binder());
    }
    catch (IOException e) {
      LOG.error("Failed to load nexusProperties", e);
    }
  }
}
