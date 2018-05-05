package com.n26.atrposki.utils.events;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import lombok.Value;

@Value
public class TimedEvent<T>{
    private long logicalTime;
    private T event;

    public TimedEvent(long logicalTime, T event) {
        this.logicalTime = logicalTime;
        this.event = event;
    }

    public TimedEvent(TimedEvent<T> other) {
        this.logicalTime = other.logicalTime;
        this.event = other.event;
    }
}
