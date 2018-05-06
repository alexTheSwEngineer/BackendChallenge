package tests;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.domain.AggregateStatistics;
import com.n26.atrposki.domain.Transaction;
import com.n26.atrposki.domain.TransactionMadeEventHandler;
import com.n26.atrposki.statistics.StatisticsService;
import com.n26.atrposki.utils.events.TimedEvent;
import com.n26.atrposki.utils.testableAtomics.AtomicLongWrapper;
import com.n26.atrposki.utils.testableAtomics.IAtomicLong;
import com.n26.atrposki.utils.time.ITimeService;
import com.n26.atrposki.utils.time.TimeServiceImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StatisticsServiceTests {
    long anyLong = 12;
    double anyDouble = 2.3;


    @Test
    public void whenEventListIsEmpty_getLastEventTimeOr_returnsDefaultValue() {
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), new TransactionMadeEventHandler(), new AggregateStatistics(), new AtomicLongWrapper());
        long defaultVal = 13;
        long actuall = statisticsService.getLastEventTimeOr(new ArrayList<>(), defaultVal);
        assertEquals(actuall, defaultVal);
    }

    @Test
    public void whenEventListIsNull_getLastEventTimeOr_returnsDefaultValue() {
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), new TransactionMadeEventHandler(), new AggregateStatistics(), new AtomicLongWrapper());
        long defaultVal = 13;
        long actuall = statisticsService.getLastEventTimeOr(null, defaultVal);
        assertEquals(actuall, defaultVal);
    }

    @Test
    public void whenEventListIsFull_getLastEventTimeOr_returnsLogicalTimeOfLastEvent() {
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), new TransactionMadeEventHandler(), new AggregateStatistics(), new AtomicLongWrapper());
        long defaultVal = 13;
        long expected = 3113;
        long actuall = statisticsService.getLastEventTimeOr(asList(new TimedEvent<>(expected, new Transaction(1.0, 1l))), defaultVal);
        assertEquals(actuall, expected);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenEventWindowToTimestampIsInvalid_InvalidArgumentExceptionIsRaised() {
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), new TransactionMadeEventHandler(), new AggregateStatistics(), new AtomicLongWrapper());
        statisticsService.calculateStatistics(null, 1, -1);
    }

    @Test
    public void whenTransactionIsAfterNow_itisNotAggregatedInStatistics() {
        long now = 1;
        long before = -1;
        long future = 2;
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), new TransactionMadeEventHandler(), new AggregateStatistics(), new AtomicLongWrapper());

        AggregateStatistics aggregateStatistics = statisticsService.calculateStatistics(asList(new TimedEvent<>(anyLong, new Transaction(anyDouble, future))), before, now);
        assertEquals(aggregateStatistics, new AggregateStatistics());

    }

    @Test
    public void givenAnEventWithOlderEventsLogicalTime_WhenEventHandlerTimeIsLaterAsIsLastUpdateTime_AggregateStatisticsIsNotUpdated() {
        long laterLogicalTime = 1000;
        long earlierLogicalTime = 10;
        List<TimedEvent<Transaction>> events = asList(new TimedEvent<>(earlierLogicalTime, new Transaction(1, 1)));

        TransactionMadeEventHandler madeEventHandler = mock(TransactionMadeEventHandler.class);
        when(madeEventHandler.getLogicalTime()).thenAnswer(x -> laterLogicalTime);
        when(madeEventHandler.getHistory()).thenAnswer(x -> events);
        AggregateStatistics statistics = new AggregateStatistics();
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), madeEventHandler, statistics, new AtomicLongWrapper(laterLogicalTime));

        long firstUpdate = statisticsService.onTransactionMade(null);


        assertEquals(firstUpdate, laterLogicalTime);
        assertSame("statistics were altered", statisticsService.getStatistics(), statistics);
    }

    @Test
    public void givenAnEvent_AggregateStatisticsGetsUpdated_callsOnTransactionMade() {
        TransactionMadeEventHandler transactionMadeEventHandler = new TransactionMadeEventHandler();
        ITimeService timeService = mock(ITimeService.class);
        when(timeService.getUtcNow()).thenAnswer(x -> anyLong);
        StatisticsService statisticsService = new StatisticsService(timeService, transactionMadeEventHandler, new AtomicLongWrapper());

        transactionMadeEventHandler.publish(new Transaction(anyDouble, anyLong));
        assertEquals(statisticsService.getStatistics().getCount(), 1);
        assertEquals(statisticsService.getStatistics().getSum(), anyDouble, 0.1);
    }

    @Test
    public void whenCalledWithStaleTime_tryUpdateDoesntUpdate() {
        long laterLogicalTime = 10;
        long earlierLogicalTime = 9;
        AggregateStatistics existing = new AggregateStatistics();
        AggregateStatistics newAggregateStatistics = new AggregateStatistics();

        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), new TransactionMadeEventHandler(), existing, new AtomicLongWrapper(laterLogicalTime));

        long updateTime = statisticsService.tryUpdateStatistics(earlierLogicalTime, newAggregateStatistics);
        assertEquals("update time is changed", updateTime, laterLogicalTime);
        assertNotSame("statistics was updated", existing, newAggregateStatistics);
        assertSame("statistics was updated", existing, statisticsService.getStatistics());
    }

    @Test
    public void whenCalledUpToDateTime_ButOtherThreadIsFaster_tryUpdateDoesntUpdate() {
        long laterLogicalTime = 10;
        long earlierLogicalTime = 9;
        AggregateStatistics existing = new AggregateStatistics();
        AggregateStatistics newAggregateStatistics = new AggregateStatistics();
        IAtomicLong lastUpdateLogicalTimeMock = mock(IAtomicLong.class);
        when(lastUpdateLogicalTimeMock.get()).thenAnswer(x -> earlierLogicalTime);
        when(lastUpdateLogicalTimeMock.compareAndSet(anyLong(), anyLong())).thenAnswer(x -> false);
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), new TransactionMadeEventHandler(), existing, lastUpdateLogicalTimeMock);

        long updateTime = statisticsService.tryUpdateStatistics(laterLogicalTime, newAggregateStatistics);
        assertNotSame("statistics was updated", existing, newAggregateStatistics);
        assertSame("statistics was updated", existing, statisticsService.getStatistics());
    }

    @Test
    public void givenEarlyEventHandlerLogicalTime_WhenLastUpdateTimeIsLater_AggregateStatisticsIsNotUpdated() {
        long laterLogicalTime = 1000;
        long earlierLogicalTime = 10;
        List<Long> reverseOrdersOfEventTImes = asList(laterLogicalTime, earlierLogicalTime);
        List<TimedEvent<Transaction>> events = asList(new TimedEvent<>(earlierLogicalTime, new Transaction(1, 1)),
                new TimedEvent<>(laterLogicalTime, new Transaction(1, 1)));
        Iterator<Long> getTimesAnswerItterator = reverseOrdersOfEventTImes.iterator();

        TransactionMadeEventHandler madeEventHandler = mock(TransactionMadeEventHandler.class);
        when(madeEventHandler.getLogicalTime()).thenAnswer(x -> getTimesAnswerItterator.next());
        when(madeEventHandler.getHistory()).thenAnswer(x -> events);

        AggregateStatistics statistics = new AggregateStatistics();
        StatisticsService statisticsService = new StatisticsService(new TimeServiceImpl(), madeEventHandler, statistics, new AtomicLongWrapper(laterLogicalTime));

        long firstUpdate = statisticsService.onTransactionMade(null);
        assertNotSame("update of statistics was not made", statisticsService.getStatistics(), statistics);
        assertEquals("last update time is not updated properly", laterLogicalTime, firstUpdate);
        AggregateStatistics secondStatistics = statisticsService.getStatistics();
        long secondUpdate = statisticsService.onTransactionMade(null);
        assertEquals("last update time is not update properly ", laterLogicalTime, secondUpdate);
        assertSame(secondStatistics, statisticsService.getStatistics());
    }

}
