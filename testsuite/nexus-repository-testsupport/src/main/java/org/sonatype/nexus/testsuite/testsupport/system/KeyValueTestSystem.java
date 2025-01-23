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
package org.sonatype.nexus.testsuite.testsupport.system;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;

@FeatureFlag(name = "nexus.test.base")
@Named
@Singleton
public class KeyValueTestSystem
    extends TestSystemSupport
{
  private final GlobalKeyValueStore keyValueStore;

  private final Map<String, NexusKeyValue> keyValuesToRestore;

  private final Collection<String> keysToClear;

  @Inject
  public KeyValueTestSystem(final GlobalKeyValueStore keyValueStore, final EventManager eventManager) {
    super(eventManager);
    this.keyValueStore = keyValueStore;
    this.keyValuesToRestore = new HashMap<>();
    this.keysToClear = new HashSet<>();
  }

  @Override
  protected void doBefore() {
    keyValuesToRestore.clear();
    keysToClear.clear();
  }

  @Override
  protected void doAfter() {
    keyValuesToRestore.forEach((key, value) -> {
      switch (value.type()) {
        case CHARACTER -> keyValueStore.setString(key, value.getAsString());
        case NUMBER -> keyValueStore.setInt(key, value.getAsInt());
        case BOOLEAN -> keyValueStore.setBoolean(key, value.getAsBoolean());
        case OBJECT -> throw new IllegalArgumentException("Object is not handled");
      }
    });

    keysToClear.forEach(key -> keyValueStore.removeKey(key));
  }

  public void setBoolean(String key, boolean value) {
    Optional<Boolean> originalValue = keyValueStore.getBoolean(key);
    if (originalValue.isPresent() && !keyValuesToRestore.containsKey(key) && !keysToClear.contains(key)) {
      keyValuesToRestore.put(key, new NexusKeyValue(key, ValueType.BOOLEAN, originalValue.get()));
    }
    else if (originalValue.isEmpty() && !keyValuesToRestore.containsKey(key)) {
      keysToClear.add(key);
    }

    keyValueStore.setBoolean(key, value);
  }

}
