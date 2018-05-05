package com.n26.atrposki.utils.time;

import org.springframework.stereotype.Component;

/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */
@Component
public class TimeServiceImpl implements ITimeService{
    @Override
    public long getUtcNow() {
        return System.currentTimeMillis();
    }
}
