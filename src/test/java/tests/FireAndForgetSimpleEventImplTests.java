package tests;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.utils.events.AggregateException;
import com.n26.atrposki.utils.events.InMemmoryThreadSafeEventHandlerImpl;
import com.n26.atrposki.utils.events.IEventHandler;
import com.n26.atrposki.utils.events.TimedEvent;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FireAndForgetSimpleEventImplTests {

    @Test
    public void GivenEronousTask_whenEventIsPublished_NoExceptionsAreRaised(){
        IEventHandler<Object> event=createSUT();
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
        IEventHandler<Object> event=createSUT();
        AtomicBoolean isInvoked = new AtomicBoolean(false);
        event.subscribe(x->isInvoked.set(true));
        event.publish(null);
        assertTrue("hapy flow listener was not called",isInvoked.get());
    }


    @Test
    public void GivenHappyFlowTask_whenEventIsPublished_CorrectMsgIsPropagated(){
        IEventHandler<Object> event=createSUT();
        Object msg = new Object();
        event.subscribe(actuallMsg->assertSame("Message is not propagated",msg,actuallMsg.getEvent()));

        event.publish(msg);
    }

    @Test
    public void GivenBothHappyFlowAndEronousTasks_whenEventIsPublished_AllHapyFLowTaskAreExecuted(){
        IEventHandler<Object> event = createSUT();
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
        InMemmoryThreadSafeEventHandlerImpl event = new InMemmoryThreadSafeEventHandlerImpl<Object>();
        event.subscribe(x->{
            throw new Exception();
        });
        event.subscribe(x->{});
        event.publishUnsafely(null);
    }

    public void whenPublished_eventHistory_savesEvent(){
        IEventHandler<Object> sut = createSUT();
        Object obj =new Object();
        sut.publish(obj);

        Collection<TimedEvent<Object>> history = sut.getHistory();

        assertTrue("expected event not found in event history",history.stream().map(x->x.getEvent()).anyMatch(x->x==obj));
    }

    private IEventHandler<Object> createSUT() {
        return new InMemmoryThreadSafeEventHandlerImpl<>();
    }

}
