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
package org.sonatype.nexus.blobstore;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to build date-based prefix based on the given date range. For more details, see
 * {@link DateBasedLocationStrategy}
 */
public class DateBasedHelper
{
  private static final Map<ChronoUnit, DateTimeFormatter> prefixFormatsByUnit = Map.of(
      ChronoUnit.DAYS, DateTimeFormatter.ofPattern("yyyy/MM/dd"),
      ChronoUnit.HOURS, DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"),
      ChronoUnit.MINUTES, DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm"));

  /**
   * Returns the date path prefix based on the given date range.
   *
   * @param fromDateTime the start date
   * @param toDateTime the end date
   * @return the date path prefix
   */
  public static String getDatePathPrefix(final OffsetDateTime fromDateTime, final OffsetDateTime toDateTime) {
    StringBuilder datePathPrefix = new StringBuilder();

    if (fromDateTime.getYear() == toDateTime.getYear()) {
      datePathPrefix.append("yyyy").append("/");
      if (fromDateTime.getMonth().getValue() == toDateTime.getMonth().getValue()) {
        datePathPrefix.append("MM").append("/");
        if (fromDateTime.getDayOfMonth() == toDateTime.getDayOfMonth()) {
          datePathPrefix.append("dd").append("/");
          if (fromDateTime.getHour() == toDateTime.getHour()) {
            datePathPrefix.append("HH").append("/");
            if (fromDateTime.getMinute() == toDateTime.getMinute()) {
              datePathPrefix.append("mm").append("/");
            }
          }
        }
      }
    }
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(datePathPrefix.toString());

    return toDateTime.format(dateTimeFormatter);
  }

  public static Map<String, DateInterval> generatePrefixes(
      final OffsetDateTime from,
      final OffsetDateTime to,
      String... additionalPrefixes)
  {
    Map<String, DateInterval> prefixes = new HashMap<>();
    OffsetDateTime startTime = from.truncatedTo(ChronoUnit.MINUTES).withOffsetSameInstant(ZoneOffset.UTC);
    OffsetDateTime endTime = to.truncatedTo(ChronoUnit.MINUTES).withOffsetSameInstant(ZoneOffset.UTC);

    Duration duration = Duration.between(startTime, endTime);
    ChronoUnit unit = (duration.toDays() > 0)
        ? ChronoUnit.DAYS
        : (duration.toHours() > 0 || duration.toMinutes() > 30)
            ? ChronoUnit.HOURS
            : ChronoUnit.MINUTES;
    DateTimeFormatter dateTimeFormatter = prefixFormatsByUnit.get(unit);

    new DateIntervalIterator(startTime, endTime, unit)
        .forEachRemaining(interval -> prefixes.put(dateTimeFormatter.format(interval.getStart()), interval));

    prefixes.putAll(Stream.of(additionalPrefixes)
        .collect(Collectors.toMap(UnaryOperator.identity(), unused -> new DateInterval(startTime, endTime))));
    return prefixes;
  }

  public static Map<String, DateInterval> generateSingletonPrefix(
      final OffsetDateTime from,
      final OffsetDateTime to,
      String prefix)
  {
    return Map.of(prefix, new DateInterval(from, to));
  }

  public static class DateInterval
  {
    private final OffsetDateTime start;

    private final OffsetDateTime end;

    public DateInterval(final OffsetDateTime start, final OffsetDateTime end) {
      this.start = start;
      this.end = end;
    }

    public OffsetDateTime getStart() {
      return start;
    }

    public OffsetDateTime getEnd() {
      return end;
    }
  }

  private static class DateIntervalIterator
      implements Iterator<DateInterval>
  {
    private final OffsetDateTime end;

    private final ChronoUnit unit;

    private OffsetDateTime currentStart;

    private OffsetDateTime currentEnd;

    private DateIntervalIterator(
        final OffsetDateTime start,
        final OffsetDateTime end,
        final ChronoUnit unit)
    {
      this.currentStart = null;
      this.currentEnd = start;
      this.end = end;
      this.unit = unit;
    }

    @Override
    public boolean hasNext() {
      return !currentEnd.isAfter(end);
    }

    @Override
    public DateInterval next() {
      currentStart = currentEnd;
      currentEnd = currentStart
          .truncatedTo(unit)
          .plus(unit.getDuration());
      return new DateInterval(currentStart, currentEnd);
    }
  }
}
