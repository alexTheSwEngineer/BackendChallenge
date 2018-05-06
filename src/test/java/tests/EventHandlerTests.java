package tests;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.utils.events.AggregateException;
import com.n26.atrposki.utils.events.InMemmoryThreadSafeLogicalyTimedEventHandlerImpl;
import com.n26.atrposki.utils.events.ILogicalyTimedEventHandler;
import com.n26.atrposki.utils.events.TimedEvent;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EventHandlerTests {

    @Test
    public void GivenEronousTask_whenEventIsPublished_NoExceptionsAreRaised(){
        ILogicalyTimedEventHandler<Object> event=createSUT();
        AtomicBoolean isInvoked = new AtomicBoolean(false);
        event.subscribe(x->{
            isInvoked.set(true);
            throw new Exception();
        });
        event.publish(null);
        assertTrue("error function was not called",isInvoked.get());
    }


    @Test
    public void GivenHappyFlowTask_whenEventIsPublished_TaskIsExecuted(){
        ILogicalyTimedEventHandler<Object> event=createSUT();
        AtomicBoolean isInvoked = new AtomicBoolean(false);
        event.subscribe(x->isInvoked.set(true));
        event.publish(null);
        assertTrue("hapy flow listener was not called",isInvoked.get());
    }


    @Test
    public void GivenHappyFlowTask_whenEventIsPublished_CorrectMsgIsPropagated(){
        ILogicalyTimedEventHandler<Object> event=createSUT();
        Object msg = new Object();
        event.subscribe(actuallMsg->assertSame("Message is not propagated",msg,actuallMsg.getEvent()));

        event.publish(msg);
    }

    @Test
    public void GivenBothHappyFlowAndEronousTasks_whenEventIsPublished_AllHapyFLowTaskAreExecuted(){
        ILogicalyTimedEventHandler<Object> event = createSUT();
        AtomicInteger count = new AtomicInteger(0);
        event.subscribe(x->{
            count.incrementAndGet();
            throw new Exception();
        });
        event.subscribe(x->count.incrementAndGet());

        event.publish(null);
        assertEquals("NOt all method calls were invoked",2,count.get());


    }

    @Test(expected = AggregateException.class)
    public void GivenBothHappyFlowAndEronousTasks_whenEventIsPublishedUnsafely_AggregateExceptionIsTHrown(){
        InMemmoryThreadSafeLogicalyTimedEventHandlerImpl event = new InMemmoryThreadSafeLogicalyTimedEventHandlerImpl<Object>();
        event.subscribe(x->{
            throw new Exception();
        });
        event.subscribe(x->{});
        event.publishUnsafely(null);
    }

    public void whenPublished_eventHistory_savesEvent(){
        ILogicalyTimedEventHandler<Object> sut = createSUT();
        Object obj =new Object();
        sut.publish(obj);

        Collection<TimedEvent<Object>> history = sut.getHistory();

        assertTrue("expected event not found in event history",history.stream().map(x->x.getEvent()).anyMatch(x->x==obj));
    }

    @Test
    public void When4eventsArePublished_getLogicalTimeReturns3_becauseItIsZeroBased(){
        ILogicalyTimedEventHandler<Object> event=createSUT();
        Object msg = new Object();
        event.publish(msg);
        event.publish(msg);
        event.publish(msg);
        event.publish(msg);
        assertEquals(3,event.getLogicalTime());
    }


    @Test
    public void GivenAPublishedEvent_whenEventHandlersAreCalled_itIsAlreadyInTheHistory(){
        ILogicalyTimedEventHandler<Object> event=createSUT();
        AtomicBoolean bool = new AtomicBoolean(false);
        Object msg = new Object();
        event.subscribe(x->{
            Object actuallMsg = x.getEvent();
            TimedEvent<Object> lastTimedEvent = event.getHistory().get(event.getHistory().size());
            Object actuallMsgFromHistory = lastTimedEvent.getEvent();
            assertSame(actuallMsg,msg);
            assertSame("Event is not added to history before it is published", actuallMsg,actuallMsgFromHistory);
        });
        event.publish(msg);
    }

    private ILogicalyTimedEventHandler<Object> createSUT() {
        return new InMemmoryThreadSafeLogicalyTimedEventHandlerImpl<>();
    }

}
