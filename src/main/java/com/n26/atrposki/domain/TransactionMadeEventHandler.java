package com.n26.atrposki.domain;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.utils.events.InMemmoryThreadSafeEventHandlerImpl;
import com.n26.atrposki.utils.events.IEventHandler;
import com.n26.atrposki.utils.events.TimedEvent;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class TransactionMadeEventHandler implements IEventHandler<Transaction> {
    private InMemmoryThreadSafeEventHandlerImpl<Transaction> eventHandlingImpl;

    public TransactionMadeEventHandler() {
        this(new InMemmoryThreadSafeEventHandlerImpl<>());
    }

    public TransactionMadeEventHandler(InMemmoryThreadSafeEventHandlerImpl<Transaction> eventHandlingImpl) {
        this.eventHandlingImpl = eventHandlingImpl;
    }

    @Override
    public <TEx extends Exception> void subscribe(CheckedConsumer<Transaction, TEx> handler) {
        eventHandlingImpl.subscribe(handler);
    }

    @Override
    public void publish(Transaction transaction) {
        eventHandlingImpl.publish(transaction);
    }

    @Override
    public Collection<TimedEvent<Transaction>> getHistory() {
        return eventHandlingImpl.getHistory();
    }
}
