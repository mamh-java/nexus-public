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
package org.sonatype.nexus.spring.application;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.app.FeatureFlags;

import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

@Configuration
public class NexusProperties
{
  // launch scripts set basedir to "../"
  public static final String BASEDIR = "";

  public static final String DATADIR = "../sonatype-work/nexus3/";

  public static final String INTERNAL_DEFAULT_PATH = "default.properties";

  public static final File EXTERNAL_DEFAULT_NEXUS_PROPERTIES_FILEPATH =
      new File(BASEDIR + "etc/nexus-default.properties");

  public static final File EXTERNAL_NEXUS_PROPERTIES_FILEPATH = new File(DATADIR + "etc/nexus.properties");

  public static final File EXTERNAL_NODENAME_FILEPATH = new File(DATADIR + "etc/nexus-nodename.properties");

  private static final Logger LOG = LoggerFactory.getLogger(NexusProperties.class);

  private PropertyMap nexusProperties;

  public PropertyMap get() throws IOException {
    if (nexusProperties == null) {
      maybeCopyDefaults();

      nexusProperties = new PropertyMap();
      applyClasspathProperties(nexusProperties);
      applyProperties(nexusProperties, EXTERNAL_DEFAULT_NEXUS_PROPERTIES_FILEPATH, true, emptySet());
      applyProperties(nexusProperties, EXTERNAL_NEXUS_PROPERTIES_FILEPATH, false, emptySet());
      applyProperties(nexusProperties, EXTERNAL_NODENAME_FILEPATH, false, singleton("nexus.clustered.nodeName"));
      applySystemProperties(nexusProperties);

      interpolate();

      canonicalize(nexusProperties, "karaf.base");
      canonicalize(nexusProperties, "karaf.etc");
      canonicalize(nexusProperties, "karaf.data");

      resolveAnyImplicitDefaults(nexusProperties);
      LOG.info("nexus.properties: {}", nexusProperties);
    }
    return nexusProperties;
  }

  private void applyClasspathProperties(final PropertyMap nexusProperties) throws IOException {
    URL resource = getClass().getResource(INTERNAL_DEFAULT_PATH);
    if (resource != null) {
      nexusProperties.load(resource);
      LOG.debug("nexus.properties after loading from classpath {}: {}", INTERNAL_DEFAULT_PATH, nexusProperties);
    }
  }

  private void applyProperties(
      final PropertyMap nexusProperties,
      final File externalFilepath,
      final boolean required,
      final Set<String> keysToWatch) throws IOException
  {
    PropertyMap props = new PropertyMap();
    try {
      props.load(externalFilepath.toURI().toURL());
      if (keysToWatch == null || keysToWatch.isEmpty()) {
        nexusProperties.putAll(props);
        LOG.debug(
            "nexus.properties after loading from file {}: {}",
            externalFilepath.getAbsolutePath(),
            nexusProperties);
      }
      else {
        for (Entry<String, String> entry : props.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          if (keysToWatch.contains(key)) {
            nexusProperties.put(key, value);
            LOG.debug("Overriding key {} with value {} from file {}", key, value, externalFilepath);
          }
        }
      }
    }
    catch (IOException e) {
      if (required) {
        throw e;
      }

      LOG.debug("Failed to load properties from non-required file: {}", externalFilepath);
    }
  }

  private void applySystemProperties(final PropertyMap nexusProperties) {
    for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
      String key = entry.getKey().toString();
      String value = entry.getValue().toString();
      if (nexusProperties.containsKey(key)) {
        nexusProperties.put(key, value);
        LOG.debug("Overriding key {} with value {} from System.properties", key, value);
      }
    }
  }

  private void maybeCopyDefaults() throws IOException {
    if (EXTERNAL_DEFAULT_NEXUS_PROPERTIES_FILEPATH.exists() && !EXTERNAL_NEXUS_PROPERTIES_FILEPATH.exists()) {
      File parentDir = EXTERNAL_NEXUS_PROPERTIES_FILEPATH.getParentFile();
      if (parentDir != null && !parentDir.isDirectory()) {
        Files.createDirectories(parentDir.toPath());
      }

      // Get list of default properties, commented out
      List<String> defaultProperties =
          getDefaultPropertiesCommentedOut(EXTERNAL_DEFAULT_NEXUS_PROPERTIES_FILEPATH.toPath());

      Files.write(EXTERNAL_NEXUS_PROPERTIES_FILEPATH.toPath(), defaultProperties, ISO_8859_1);
    }
  }

  private List<String> getDefaultPropertiesCommentedOut(final Path defaultPropertiesPath) throws IOException {
    return Files.readAllLines(defaultPropertiesPath, ISO_8859_1)
        .stream()
        .filter(l -> !l.startsWith("##"))
        .map(l -> l.startsWith("#") || l.isEmpty() ? l : "# " + l)
        .collect(Collectors.toList());
  }

  private void canonicalize(final PropertyMap nexuProperties, final String name) throws IOException {
    String value = nexuProperties.get(name);
    if (value == null) {
      LOG.warn("Unable to canonicalize null entry: {}", name);
      return;
    }
    File file = new File(value).getCanonicalFile();
    nexuProperties.put(name, file.getPath());
  }

  private void interpolate() throws IOException {
    Interpolator interpolator = new StringSearchInterpolator();
    interpolator.addValueSource(new MapBasedValueSource(nexusProperties));
    interpolator.addValueSource(new MapBasedValueSource(System.getProperties()));
    interpolator.addValueSource(new EnvarBasedValueSource());

    for (Entry<String, String> entry : nexusProperties.entrySet()) {
      try {
        nexusProperties.put(entry.getKey(), interpolator.interpolate(entry.getValue()));
      }
      catch (InterpolationException e) {
        throw new RuntimeException("Failed to interpolate nexus.properties entry: {}/{}", e);
      }
    }
  }

  private void resolveAnyImplicitDefaults(final PropertyMap nexusProperties) {
    // if neither SESSION_ENABLED or JWT_ENABLED is set, default to session
    if (!nexusProperties.containsKey(FeatureFlags.SESSION_ENABLED)
        && !nexusProperties.containsKey(FeatureFlags.JWT_ENABLED)) {
      nexusProperties.put(FeatureFlags.SESSION_ENABLED, TRUE.toString());
      nexusProperties.put(FeatureFlags.JWT_ENABLED, FALSE.toString());
    }
    else if (nexusProperties.containsKey(FeatureFlags.SESSION_ENABLED)
        && !nexusProperties.containsKey(FeatureFlags.JWT_ENABLED)) {
      nexusProperties.put(FeatureFlags.JWT_ENABLED,
          String.valueOf(!Boolean.parseBoolean(nexusProperties.get(FeatureFlags.SESSION_ENABLED))));
    }
    else if (nexusProperties.containsKey(FeatureFlags.JWT_ENABLED)
        && !nexusProperties.containsKey(FeatureFlags.SESSION_ENABLED)) {
      nexusProperties.put(FeatureFlags.SESSION_ENABLED,
          String.valueOf(!Boolean.parseBoolean(nexusProperties.get(FeatureFlags.JWT_ENABLED))));
    }

    // onboarding enabled
    if (!nexusProperties.containsKey("nexus.onboarding.enabled")) {
      nexusProperties.put("nexus.onboarding.enabled", TRUE.toString());
    }

    if (!nexusProperties.containsKey("nexus.scripts.allowCreation")) {
      nexusProperties.put("nexus.scripts.allowCreation", FALSE.toString());
    }

    if (!nexusProperties.containsKey("nexus.http.denyframe.enabled")) {
      nexusProperties.put("nexus.http.denyframe.enabled", TRUE.toString());
    }

    if (Boolean.parseBoolean(nexusProperties.get(FeatureFlags.DATASTORE_CLUSTERED_ENABLED))) {
      // if datastore is clustered, JWT must be enabled
      nexusProperties.put(FeatureFlags.JWT_ENABLED, TRUE.toString());
      nexusProperties.put(FeatureFlags.SESSION_ENABLED, FALSE.toString());
    }
  }
}
