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
package org.sonatype.nexus.inject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.sonatype.nexus.common.app.FeatureFlag;

@SupportedAnnotationTypes("org.sonatype.nexus.common.app.FeatureFlag")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class FeatureFlagAnnotationProcessor
    extends AbstractProcessor
{

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      return false;
    }

    try {
      FileObject file = processingEnv.getFiler()
          .createResource(StandardLocation.CLASS_OUTPUT, "sonatype", "org.sonatype.nexus.common.app.FeatureFlag");
      try (PrintWriter writer = new PrintWriter(file.openWriter())) {
        for (Element element : roundEnv.getElementsAnnotatedWith(FeatureFlag.class)) {
          FeatureFlag featureFlag = element.getAnnotation(FeatureFlag.class);
          // first element is the classname
          String line = element.toString() +
              "/" +
              // second element is the feature flag name
              featureFlag.name() +
              "/" +
              // third element is the enabledByDefault flag
              featureFlag.enabledByDefault() +
              "/" +
              // fourth element is the inverse flag
              featureFlag.inverse();
          writer.println(line);
        }
      }
    }
    catch (IOException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
    }

    return true;
  }
}
