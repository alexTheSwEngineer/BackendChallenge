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
 *
 */

import java.util.Collection;

public interface IEventHandler<TMsg> {
    @FunctionalInterface
     static interface CheckedConsumer<Tmsg,TEx extends Exception>{
         void apply(TimedEvent<Tmsg> msg) throws Exception;
    }

     <TEx extends Exception> void  subscribe(CheckedConsumer<TMsg,TEx> handler);
     void publish(TMsg msg);
     Collection<TimedEvent<TMsg>> getHistory();
}
