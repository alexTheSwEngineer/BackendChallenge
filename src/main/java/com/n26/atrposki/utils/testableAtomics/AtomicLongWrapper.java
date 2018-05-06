package com.n26.atrposki.utils.testableAtomics;
/*
 * @author aleksandartrposki@gmail.com
 * @since 06.05.18
 *
 *
 */

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongWrapper implements IAtomicLong {
    private AtomicLong impl;

    public AtomicLongWrapper() {
        this(new AtomicLong(0));
    }

    public AtomicLongWrapper(long initVal) {
        this(new AtomicLong(initVal));
    }

    public AtomicLongWrapper(AtomicLong impl) {
        this.impl = impl;
    }

    @Override
    public boolean compareAndSet(long expected, long newVal) {
        return impl.compareAndSet(expected,newVal);
    }

    @Override
    public long get() {
        return impl.get();
    }

    @Override
    public void forceSet(long val) {
         impl.set(val);
    }
}
