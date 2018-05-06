package com.n26.atrposki.utils.testableAtomics;
/*
 * @author aleksandartrposki@gmail.com
 * @since 06.05.18
 *
 *
 */

public interface IAtomicLong {
     boolean compareAndSet(long expected, long newVal);
     long get();
     void forceSet(long val);
}
