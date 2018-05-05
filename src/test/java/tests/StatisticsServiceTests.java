package tests;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.domain.Transaction;
import com.n26.atrposki.domain.TransactionMadeEventHandler;
import com.n26.atrposki.statistics.StatisticsService;
import com.n26.atrposki.utils.events.TimedEvent;
import com.n26.atrposki.utils.time.ITimeService;
import com.n26.atrposki.utils.time.TimeServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.Invocation;

import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StatisticsServiceTests {
    long anyLong  =12;
    double anyDouble =2.3;

    @Test
    public void givenOlderEvents_AggregateStatisticsIsNotUpdated(){
        TransactionMadeEventHandler madeEventHandler = new TransactionMadeEventHandler();
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), madeEventHandler);
        long laterLogicalTime = 1000;
        long earlierLogicalTime = 10;

        long firstUpdate = statisticsService.onTransactionMade(new TimedEvent<>(laterLogicalTime,new Transaction(anyDouble,anyLong)));
        assertEquals(firstUpdate,laterLogicalTime);
        long secondUpdate = statisticsService.onTransactionMade(new TimedEvent<>(earlierLogicalTime,new Transaction(anyDouble,anyLong)));
        assertEquals(secondUpdate,laterLogicalTime);
    }

    @Test
    public void givenAnEvent_AggregateStatisticsGetsUpdated_callsOnTransactionMade(){
        TransactionMadeEventHandler transactionMadeEventHandler = new TransactionMadeEventHandler();
        ITimeService timeService = mock(ITimeService.class);
        when(timeService.getUtcNow()).thenAnswer(x->anyLong);
        StatisticsService statisticsService = new StatisticsService(timeService, transactionMadeEventHandler);

        transactionMadeEventHandler.publish(new Transaction(anyDouble,anyLong));
        assertEquals(statisticsService.getStatistics().getCount(),1);
        assertEquals(statisticsService.getStatistics().getSum(),anyDouble,0.1);
    }
}
