package com.n26.atrposki.utils.events;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toList;

/**
 * Core implementation of the Event interface /Observer pattern.
 * <p>
 * It has pessimistic expectations about the listeners methods raising exceptions and is made deliberetly robust against exceptions.
 * It has however, optimistic expectations about listeners not halting, since it is executed on the callers thread.
 * This naive implementation keeps infinite event history which might prove to be an issue in busy systems. A sliding window solution may be more appropriate with high throughput systems.
 */
public class InMemmoryThreadSafeLogicalyTimedEventHandlerImpl<TMsg> implements ILogicalyTimedEventHandler<TMsg> {
    Set<CheckedConsumer<TMsg, Exception>> listeners = ConcurrentHashMap.newKeySet();
    ConcurrentLinkedQueue eventHistory = new ConcurrentLinkedQueue();
    AtomicLong logicalTime = new AtomicLong(-1);

    public InMemmoryThreadSafeLogicalyTimedEventHandlerImpl() {
        this.listeners = ConcurrentHashMap.newKeySet();
        this.eventHistory = new ConcurrentLinkedQueue();
        this.logicalTime = new AtomicLong(-1);
    }

    public InMemmoryThreadSafeLogicalyTimedEventHandlerImpl(Set<CheckedConsumer<TMsg, Exception>> listeners, ConcurrentLinkedQueue eventHistory, AtomicLong logicalTime) {
        this.listeners = listeners;
        this.eventHistory = eventHistory;
        this.logicalTime = logicalTime;
    }

    /**
     * Registers an event handler to listen for publishing of this event
     *
     * @param handler the event handler to be called when this event is raised
     */
    @Override
    public <TEx extends Exception> void subscribe(CheckedConsumer<TMsg, TEx> handler) {
        listeners.add(handler::apply);
    }

    /**
     * Calls {@link InMemmoryThreadSafeLogicalyTimedEventHandlerImpl#publishUnsafely(Object)} and logs any Aggregate exceptions but doesn't propagate them
     *
     * @param msg a message to be passed to all the event handlers that are subscribed. It will be wrapped in a TimedEvent together with a loggical incremental time
     */
    @Override
    public void publish(TMsg msg) {
        try {
            publishUnsafely(msg);
        } catch (AggregateException a) {
            //This type of exception handling is not a best practice.
            // A) not the exceptions responsibility to log itself.
            // B) kinda swallowing exceptions into the void, however this whole class is implemented naively as a showcase rather then a production ready code
            a.printStackTrace(System.out);
        }
    }

    /**
     * @returns a copy of the history of events since the inception of this object. This method is thread safe.
     */
    @Override
    public List<TimedEvent<TMsg>> getHistory() {
        return new ArrayList<TimedEvent<TMsg>>(eventHistory);
    }

    @Override
    public long getLogicalTime() {
        return logicalTime.get();
    }


    /**
     * Calls all the handlers with a timed event as a parameter. It will consist of the msg as the body field and autoincremented logical time as the logical time field.
     * This method is thread safe.
     * Every event has an unique eventLogicalTime which is sequentially updated starting from 0.
     * It expects none of the handlers to halt, however they are allowed to raise errors.
     * If any handlers raise errors they will be agregated into an  {@link AggregateException} that will be thrown
     *
     * @param msg the message passed to all the handlers
     * @return a timed event that was handled by all the handlers
     * @throws AggregateException an aggregate exception of all the event handlers invocations
     */
    public TimedEvent<TMsg> publishUnsafely(TMsg msg) throws AggregateException {
        Long currentLogicalTime = logicalTime.incrementAndGet();
        TimedEvent<TMsg> timedEvent = new TimedEvent<>(currentLogicalTime, msg);
        eventHistory.add(timedEvent);

        //Create snapshot of the collection to avoid concurency issues
        List<CheckedConsumer<TMsg, Exception>> handlersSnapshot = new ArrayList<>(listeners);

        List<Exception> exceptions = handlersSnapshot.stream()
                .map(x -> tryApply(x, timedEvent))
                .filter(Objects::nonNull)
                .collect(toList());

        //This is best resolved by getting another publishingStrategy parameter
        //We don't need to set in stone that every listener will be called but an agregate exception will be thrown.
        //Maybe we need to notiffy only listeners up until the first exception
        if (!exceptions.isEmpty()) {
            throw new AggregateException("Exceptions occured while publishing event", exceptions);
        }

        return timedEvent;
    }

    private Exception tryApply(CheckedConsumer<TMsg, Exception> listener, TimedEvent<TMsg> event) {
        try {
            listener.apply(event);
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}
