package com.n26.atrposki.utils.events;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 * Simple event subscribe/publish interface. In reality we need unsubscribe too.
 * <p> The idea of this interface is to provide abstraction about passing messages.
 * We can later decide if we want that to be blocking, fire and forget, if it should run on the same thread of the caller or even on the same machine.
 * There are a lot of out of the box stuff that do this: JMS topics and Queues, plain old java Observer abd Observable, JavaRx and the Flux spring boot 2.x APIs etc.
 * This interface allows us to easilly change our mind about it, since it is bound to what we logicaly need and not to the way we are going to implement it.
 * Having such really utils interface writen from scratch is in most part was due to my unsertainty of how I would implement it.
 * In production I would rather use some of the existing java libraries.
 * Another discussion to be wheter events should be enums/singletons/classes/topics/keys in a hash map etc.
 *
 */

import java.util.List;

public interface ILogicalyTimedEventHandler<TMsg> {
    @FunctionalInterface
    static interface CheckedConsumer<Tmsg, TEx extends Exception> {
        void apply(TimedEvent<Tmsg> msg) throws Exception;
    }

    /**
     * Subscribes a handler to the events of the type this Event Handlers handles.
     * @param <TEx> the type of the checked exception the handler might throw
     */
    <TEx extends Exception> void subscribe(CheckedConsumer<TMsg, TEx> handler);

    /**
     * Notifies all listeners. Every listener will be called with a parameter TimedEvent<TMsg> with appropriate logical time assosated to it.
     * @param msg the event load to be published to all lsiteners
     */
    void publish(TMsg msg);

    /**
     * @return All event history sofar.
     * */
    List<TimedEvent<TMsg>> getHistory();

    long getLogicalTime();
}
