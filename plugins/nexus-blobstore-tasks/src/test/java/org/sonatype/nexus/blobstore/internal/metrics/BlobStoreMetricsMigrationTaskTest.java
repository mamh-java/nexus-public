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
package org.sonatype.nexus.blobstore.internal.metrics;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsPropertiesReader;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BlobStoreMetricsMigrationTaskTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStoreMetricsStore metricsStore;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreMetricsPropertiesReader<?> propertiesReader;

  private BlobStoreMetrics blobStoreMetrics = new AccumulatingBlobStoreMetrics(1, 1, Map.of("default", 1l), false);

  private OperationMetrics operationMetrics = new OperationMetrics();

  private Map<String, Provider<BlobStoreMetricsPropertiesReader<?>>> metricsPropertiesReaders;

  private BlobStoreMetricsMigrationTask task;

  @Before
  public void setUp() {
    metricsPropertiesReaders = new HashMap<>();
    metricsPropertiesReaders.put("default", () -> propertiesReader);
    task = new BlobStoreMetricsMigrationTask(blobStoreManager, metricsStore, metricsPropertiesReaders);
  }

  @Test
  public void testExecuteBlobStoreNotStarted() {
    when(blobStore.isStarted()).thenReturn(false);
    BlobStoreConfiguration configuration = mockBlobStoreConfiguration("default", "testBlobStore");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);

    task.execute(blobStore);

    verify(blobStore, times(1)).isStarted();
    verifyNoMoreInteractions(metricsStore);
  }

  @Test
  public void testExecutePropertiesReaderNotFound() {
    when(blobStore.isStarted()).thenReturn(true);
    BlobStoreConfiguration configuration = mockBlobStoreConfiguration("unknown", "testBlobStore");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);

    task.execute(blobStore);

    verify(blobStore, times(1)).isStarted();
    verifyNoMoreInteractions(metricsStore);
  }

  @Test
  public void testExecuteSuccessfulMigration() throws Exception {
    when(blobStore.isStarted()).thenReturn(true);
    BlobStoreConfiguration configuration = mockBlobStoreConfiguration("default", "testBlobStore");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    when(metricsStore.get("testBlobStore")).thenReturn(new BlobStoreMetricsEntity());
    when(propertiesReader.getMetrics()).thenReturn(blobStoreMetrics);
    when(propertiesReader.getOperationMetrics()).thenReturn(
        Map.of(OperationType.DOWNLOAD, operationMetrics, OperationType.UPLOAD, operationMetrics));

    task.execute(blobStore);

    verify(blobStore, times(1)).isStarted();
    verify(metricsStore, times(1)).initializeMetrics("testBlobStore");
    verify(metricsStore, times(1)).updateMetrics(any(BlobStoreMetricsEntity.class));
  }

  @Test
  public void testExecuteMetricsFromFileIsNull() throws Exception {
    when(blobStore.isStarted()).thenReturn(true);
    BlobStoreConfiguration configuration = mockBlobStoreConfiguration("default", "testBlobStore");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    when(propertiesReader.getMetrics()).thenReturn(null);
    when(propertiesReader.getOperationMetrics()).thenReturn(
        Map.of(OperationType.DOWNLOAD, operationMetrics, OperationType.UPLOAD, operationMetrics));

    task.execute(blobStore);

    verify(blobStore, times(1)).isStarted();
    verifyNoMoreInteractions(metricsStore);
  }

  @Test
  public void testExecuteOperationMetricsIsNull() throws Exception {
    when(blobStore.isStarted()).thenReturn(true);
    BlobStoreConfiguration configuration = mockBlobStoreConfiguration("default", "testBlobStore");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    when(propertiesReader.getMetrics()).thenReturn(blobStoreMetrics);
    when(propertiesReader.getOperationMetrics()).thenReturn(null);

    task.execute(blobStore);

    verify(blobStore, times(1)).isStarted();
    verifyNoMoreInteractions(metricsStore);
  }

  @Test
  public void testExecuteMetricsFromFileAndOperationMetricsAreNull() throws Exception {
    when(blobStore.isStarted()).thenReturn(true);
    BlobStoreConfiguration configuration = mockBlobStoreConfiguration("default", "testBlobStore");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    when(propertiesReader.getMetrics()).thenReturn(null);
    when(propertiesReader.getOperationMetrics()).thenReturn(null);

    task.execute(blobStore);

    verify(blobStore, times(1)).isStarted();
    verifyNoMoreInteractions(metricsStore);
  }

  private BlobStoreConfiguration mockBlobStoreConfiguration(String type, String name) {
    BlobStoreConfiguration configuration = mock(BlobStoreConfiguration.class);
    when(configuration.getType()).thenReturn(type);
    when(configuration.getName()).thenReturn(name);
    return configuration;
  }
}
