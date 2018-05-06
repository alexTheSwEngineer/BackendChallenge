package com.n26.atrposki.domain;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.utils.events.InMemmoryThreadSafeLogicalyTimedEventHandlerImpl;
import com.n26.atrposki.utils.events.ILogicalyTimedEventHandler;
import com.n26.atrposki.utils.events.TimedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * In memory transaction made event handler.
 * Components are by default in singleton scope  and this implementation depends heavily on it.
 * */
@Component
public class TransactionMadeEventHandler implements ILogicalyTimedEventHandler<Transaction> {
    private InMemmoryThreadSafeLogicalyTimedEventHandlerImpl<Transaction> eventHandlingImpl;

    public TransactionMadeEventHandler() {
        this(new InMemmoryThreadSafeLogicalyTimedEventHandlerImpl<>());
    }

    public TransactionMadeEventHandler(InMemmoryThreadSafeLogicalyTimedEventHandlerImpl<Transaction> eventHandlingImpl) {
        this.eventHandlingImpl = eventHandlingImpl;
    }

    /**
     * Propagates calls to {@link InMemmoryThreadSafeLogicalyTimedEventHandlerImpl#subscribe(CheckedConsumer)}
     * */
    @Override
    public <TEx extends Exception> void subscribe(CheckedConsumer<Transaction, TEx> handler) {
        eventHandlingImpl.subscribe(handler);
    }

    /**
     * Propagates calls to {@link InMemmoryThreadSafeLogicalyTimedEventHandlerImpl#publish(Object)}
     * */
    @Override
    public void publish(Transaction transaction) {
        eventHandlingImpl.publish(transaction);
    }

    /**
     * Propagates calls to {@link InMemmoryThreadSafeLogicalyTimedEventHandlerImpl#getHistory()}
     * */
    @Override
    public List<TimedEvent<Transaction>> getHistory() {
        return eventHandlingImpl.getHistory();
    }

    /**
     * Propagates calls to {@link InMemmoryThreadSafeLogicalyTimedEventHandlerImpl#getLogicalTime()}
     * */
    @Override
    public long getLogicalTime() {
        return eventHandlingImpl.getLogicalTime();
    }
}
