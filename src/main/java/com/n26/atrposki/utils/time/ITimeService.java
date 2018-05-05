package com.n26.atrposki.utils.time;

import org.springframework.stereotype.Component;

/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */
@Component()
public interface ITimeService {
    public static final long MILISECONDS_IN_MINUTE = 60*1000;
    long getUtcNow();
}
