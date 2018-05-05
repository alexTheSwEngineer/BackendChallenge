package com.n26.atrposki.domain;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import lombok.Value;

@Value
public class AggregateStatistics {
    private final double sum;
    private final double max;
    private final double min;
    private final long count;
    private final double average;

    public AggregateStatistics() {
        this(0.0, 0.0, 0.0, 0.0, 0l);
    }

    public AggregateStatistics(Double sum, Double avg, Double max, Double min, Long count) {
        this.sum = sum == null ? 0 : sum;
        this.max = max == null ? 0 : max;
        this.average = avg == null ? 0 : avg;
        this.min = min == null ? 0 : min;
        this.count = count == null ? 0 : count;
    }
}
