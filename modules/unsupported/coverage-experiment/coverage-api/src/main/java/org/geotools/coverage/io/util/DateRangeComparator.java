package org.geotools.coverage.io.util;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.geotools.util.DateRange;
import org.geotools.util.Utilities;

/**
 * A DateRange comparator
 */
public class DateRangeComparator implements Comparator<DateRange> {

    @Override
    public int compare( DateRange firstDateRange, DateRange secondDateRange ) {
        Utilities.ensureNonNull("firstDateRange", firstDateRange);
        Utilities.ensureNonNull("secondDateRange", secondDateRange);
        final long beginFirst = firstDateRange.getMinValue().getTime();
        final long endFirst = firstDateRange.getMaxValue().getTime();
        final long beginSecond = secondDateRange.getMinValue().getTime();
        final long endSecond = secondDateRange.getMaxValue().getTime();
        return NumberRangeComparator.doubleCompare(beginFirst, endFirst, beginSecond, endSecond);
    }

    public static TreeSet<DateRange> fromExisting( Set<DateRange> temporalSet ) {
        TreeSet<DateRange> treeSet = new TreeSet<DateRange>(new DateRangeComparator());
        treeSet.addAll(temporalSet);
        return treeSet;
    }
}