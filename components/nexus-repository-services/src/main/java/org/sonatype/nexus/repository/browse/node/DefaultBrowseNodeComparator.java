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
package org.sonatype.nexus.repository.browse.node;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.VersionComparator;

import java.util.regex.Pattern;

/**
 * Sort using VersionComparator when dealing with two components, fall back to node name when dealing
 * with any other comparison of the same type (component/asset/folder), finally fall back to node type
 *
 * @since 3.13
 */
@Named(value = DefaultBrowseNodeComparator.NAME)
public class DefaultBrowseNodeComparator
    implements BrowseNodeComparator
{
  public static final String NAME = "default";

  private final VersionComparator versionComparator;

  private static final int TYPE_COMPONENT = 1;

  private static final int TYPE_FOLDER = 2;

  private static final int TYPE_ASSET = 3;

  private static final Pattern VERSION_RE = Pattern.compile("^\\d+$", Pattern.CASE_INSENSITIVE);

  @Inject
  public DefaultBrowseNodeComparator(final VersionComparator versionComparator) {
    this.versionComparator = versionComparator;
  }

  @Override
  public int compare(final BrowseNode o1, final BrowseNode o2) {
    int o1Type = getType(o1);
    int o2Type = getType(o2);

    if (o1Type == TYPE_COMPONENT && o2Type == TYPE_COMPONENT) {
      try {
        return versionComparator.compare(o1.getName(), o2.getName());
      }
      catch (IllegalArgumentException e) { //NOSONAR
        return 0;
      }
    }

    if (o1Type == o2Type) {
      if(isNumberLike(o1.getName()) && isNumberLike(o2.getName())){
        int number1 = Integer.parseInt( o1.getName() );
        int number2 = Integer.parseInt( o2.getName() );
        return Integer.compare(number1, number2);
      } else {
        return o1.getName().compareToIgnoreCase(o2.getName());
      }
    }

    return Integer.compare(o1Type, o2Type);
  }

  protected int getType(final BrowseNode browseNode) {
    if (browseNode.getComponentId() != null) {
      return TYPE_COMPONENT;
    }

    if (browseNode.getAssetId() != null) {
      return TYPE_ASSET;
    }

    return TYPE_FOLDER;
  }

  private boolean isNumberLike(final String version) {
    return VERSION_RE.matcher(version).matches();
  }

}
