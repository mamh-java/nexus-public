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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.spring.application.NexusProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Character.isLowerCase;

/**
 * Filter for class finder that filters out classes based on feature flags being enabled for any class that has a
 * FeatureFlag annotation, or any class in a package that has a FeatureFlag annotation.
 */
public class FeatureFlagEnabledClassFinderFilter
    implements ClassFinderFilter
{
  protected static final Logger LOG = LoggerFactory.getLogger(FeatureFlagEnabledClassFinderFilter.class);

  private final File featureFlagCacheFile;

  private final NexusProperties nexusProperties;

  private final Map<String, FeatureFlagEntry> featureFlagEntryMap = new HashMap<>();

  private final List<String> featureFlagDisabledPackages = new ArrayList<>();

  private final List<String> featureFlagDisabledClasses = new ArrayList<>();

  private static FeatureFlagEnabledClassFinderFilter instance;

  public static FeatureFlagEnabledClassFinderFilter instance(
      final File indexCacheDirectory,
      final NexusProperties nexusProperties)
  {
    if (instance == null) {
      instance = new FeatureFlagEnabledClassFinderFilter(indexCacheDirectory, nexusProperties);
    }

    return instance;
  }

  /**
   * Use the static instance() method to get an instance of this class. Since injection isn't available at this point
   * we need to manually treat this class as a singleton
   */
  private FeatureFlagEnabledClassFinderFilter(final File indexCacheDirectory, final NexusProperties nexusProperties) {
    this.featureFlagCacheFile = new File(indexCacheDirectory, "sisu/feature-flags.index");
    this.nexusProperties = nexusProperties;
    parseFeatureFlagCacheFile();
  }

  protected void parseFeatureFlagCacheFile() {
    try {
      List<String> featureFlags = Files.readAllLines(this.featureFlagCacheFile.toPath());
      for (String line : featureFlags) {
        if (line.startsWith("-")) {
          continue;
        }
        initializeFeatureFlag(new FeatureFlagEntry(line));
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to read feature flag cache " + featureFlagCacheFile, e);
    }
  }

  private void initializeFeatureFlag(final FeatureFlagEntry featureFlagEntry) {
    try {
      boolean propertyEnabled = isFeatureFlagEnabled(featureFlagEntry);
      if (propertyEnabled) {
        LOG.debug("Found enabled Feature flag {}", featureFlagEntry);
      }
      else {
        featureFlagEntryMap.put(featureFlagEntry.featureFlag, featureFlagEntry);
        if (featureFlagEntry.isPackage()) {
          initializePackageFeatureFlag(featureFlagEntry);
        }
        else {
          initializeClassFeatureFlag(featureFlagEntry);
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to initialize feature flag cache " + featureFlagCacheFile, e);
    }
  }

  private void initializeClassFeatureFlag(final FeatureFlagEntry featureFlagEntry) {
    String classToAdd = "/" + featureFlagEntry.className.replace(".", "/") + ".class";
    if (featureFlagEntryMap.containsKey(classToAdd)) {
      maybeLogFeatureFlagAnnotationConflict(featureFlagEntry);
    }
    LOG.debug("Found disabled feature flag {}", featureFlagEntry);
    featureFlagDisabledClasses.add(classToAdd);
  }

  private void initializePackageFeatureFlag(final FeatureFlagEntry featureFlagEntry) {
    String packageToAdd = "/" + featureFlagEntry.className.replace(".", "/");
    if (featureFlagEntryMap.containsKey(packageToAdd)) {
      maybeLogFeatureFlagAnnotationConflict(featureFlagEntry);
    }
    else {
      LOG.debug("Found disabled feature flag {} for package {}", featureFlagEntry.featureFlag, packageToAdd);
      featureFlagDisabledPackages.add(packageToAdd);
    }
  }

  private void maybeLogFeatureFlagAnnotationConflict(final FeatureFlagEntry newFeatureFlagEntry) {
    FeatureFlagEntry existingFeatureFlagEntry = featureFlagEntryMap.get(newFeatureFlagEntry.featureFlag);
    if (existingFeatureFlagEntry.enabledByDefault != newFeatureFlagEntry.enabledByDefault) {
      LOG.debug(
          "Found multiple uses of the same @FeatureFlag({}) with different default values!",
          newFeatureFlagEntry.featureFlag);
    }
  }

  private boolean isFeatureFlagEnabled(final FeatureFlagEntry featureFlagEntry) throws IOException {
    String featureFlagEntryValue = nexusProperties.get().get(featureFlagEntry.featureFlag);
    if (featureFlagEntryValue == null) {
      return featureFlagEntry.inverse != featureFlagEntry.enabledByDefault;
    }
    else {
      boolean result = Boolean.parseBoolean(featureFlagEntryValue);
      return featureFlagEntry.inverse != result;
    }
  }

  @Override
  public boolean allowed(final String path) {
    String[] segments = path.split("!");
    String className = "/" + segments[segments.length - 1];
    if (isFeatureFlaggedClassEnabled(className)) {
      LOG.debug(
          "Not filtering out class {}, it's @FeatureFlag (or a parent package @FeatureFlag) is enabled",
          className);
      return true;
    }
    LOG.debug(
        "Filtering out class {}, it's @FeatureFlag (or a parent package @FeatureFlag) is NOT enabled",
        className);
    return false;
  }

  private boolean isFeatureFlaggedClassEnabled(final String className) {
    LOG.debug("Checking if class {} has a disabled feature flag", className);
    if (featureFlagDisabledClasses.contains(className)) {
      return false;
    }

    LOG.debug("Checking if class {} is in a package with a disabled feature flag", className);
    for (String featureFlaggedOutPackage : featureFlagDisabledPackages) {
      if (className.startsWith(featureFlaggedOutPackage)) {
        return false;
      }
    }
    return true;
  }

  /**
   * org.sonatype.nexus.rapture.internal.RaptureJwtModule/nexus.jwt.enabled/false/false
   */
  private static class FeatureFlagEntry
  {
    final String className;

    final String featureFlag;

    final boolean enabledByDefault;

    final boolean inverse;

    private FeatureFlagEntry(String line) {
      String[] split = line.split("/");
      this.className = split[0];
      this.featureFlag = split[1];
      this.enabledByDefault = Boolean.parseBoolean(split[2]);
      this.inverse = Boolean.parseBoolean(split[3]);
    }

    private boolean isPackage() {
      String[] onlyClassnameParts = className.split("\\.");
      String onlyClassname = onlyClassnameParts[onlyClassnameParts.length - 1];
      char leadingChar = onlyClassname.toCharArray()[0];
      return isLowerCase(leadingChar);
    }

    @Override
    public String toString() {
      return "FeatureFlagEntry{" +
          "className='" + className + '\'' +
          ", featureFlag='" + featureFlag + '\'' +
          ", enabledByDefault=" + enabledByDefault +
          ", inverse=" + inverse +
          '}';
    }
  }
}
