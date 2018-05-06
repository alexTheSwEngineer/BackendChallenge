package com.n26.atrposki.statistics;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.domain.AggregateStatistics;
import com.n26.atrposki.domain.Transaction;
import com.n26.atrposki.domain.TransactionMadeEventHandler;
import com.n26.atrposki.utils.events.TimedEvent;
import com.n26.atrposki.utils.testableAtomics.IAtomicLong;
import com.n26.atrposki.utils.time.ITimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collector;

import static com.n26.atrposki.utils.time.ITimeService.MILISECONDS_IN_MINUTE;
import static java.util.stream.Collectors.*;

/**
 * A service that gets the aggregated statistics for the transactions in the last 60 seconds.
 * It gets the value from an inmemory (heap).
 * It keeps the value up to date on every transaction event.
 * Right now the underlying mechanism implemented is simply the most naive impleentation of a "in memory materialzied" view.
 * Every time a transaction hapenes the aggregated statistics are updated.
 * 2 giant assumptions:
 * a) This will not go into production and hence no memory overflow will happen due to the ineficient way the statistics are calculated on every insert
 * b) There will be much more reads then writes. (if nothing happenes in 60 seconds, no updates will occur making the statistics stale).
 * <p>
 * A simple way to fight this is to:
 * a) Keep only a sliding widnow of relevant events having O(1) update sliding window time and O(n) memory complexity
 * b) Schedule transaction expired events, or regular updates in a resonable interval.
 * <p>
 * If breaking the O(1) memory constrain is a possibility we can keep min/max predecessors so each insert/update/read will be O(1) however we will need O(n) memory.
 * Where N is not from the begining of time, but the number of transactions in a 60  sec window.
 */
@Service
public class StatisticsService {
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsService.class);
    private ITimeService timeService;
    private TransactionMadeEventHandler transactionMadeEventHandler;
    private AggregateStatistics statistics;
    private IAtomicLong lastUpdateLogicalTime;

    @Autowired
    public StatisticsService(ITimeService timeService, TransactionMadeEventHandler transactionMadeEventHandler, IAtomicLong atomicLong) {
        atomicLong.forceSet(0l);
        this.timeService = timeService;
        this.transactionMadeEventHandler = transactionMadeEventHandler;
        lastUpdateLogicalTime = atomicLong;
        statistics = new AggregateStatistics();
        transactionMadeEventHandler.subscribe(this::onTransactionMade);
    }

    public StatisticsService(ITimeService timeService, TransactionMadeEventHandler transactionMadeEventHandler, AggregateStatistics statistics, IAtomicLong lastUpdateLogicalTime) {
        this.timeService = timeService;
        this.transactionMadeEventHandler = transactionMadeEventHandler;
        this.statistics = statistics;
        this.lastUpdateLogicalTime = lastUpdateLogicalTime;
    }

    public AggregateStatistics getStatistics() {
        return statistics;
    }

    /**
     * This method is envoked on every transaction event an updates the AggregationStatistics if needed by recalculating them from the history of events (from the begining of time).
     * It handles concurency by only recalculating the events if they happened (in logical time) after the last update and no other thread  was quicker.
     * Start of a rant unimportant for the javadoc:
     * a) We can safelly assume that the transactionMadeEventHandlers logical time is only going to increment.
     * However we cannot safelly assume that from the line where we read the time, to the line where we get the events it hasnt changed.
     * Hence the two similar fail fast ifs. If we have a stale time when we are getting the logical time of the event handler, we don't need to even get the events.
     * BUT, if we did need to read the event history and we realized that we are looking at a stale history snapshot then we don't need to run the calculations.
     * Only after we ran the calculation can we go into the syncronized partts that will be kept as lean as possible to avoid performance issues.
     * b) There are more efficient ways of doing the calculation (example keeping a smaller window ordered by timestampd transactions instead of using all of them since the begining of time).
     * c) A giant assumption is that we get a constant bussy stream of transactions. This class doesn't update anything unless there is a transaction. We can tackle this by scheduled forced updates
     * or scheduling a TransactionExpiredEvent 60 seconds after recieving a transaction made event.
     * @param transaction a timed event
     * @return the new logical time for the last update of the AggregateStatistics value.
     */
    public long onTransactionMade(TimedEvent<Transaction> transaction) {
        LOG.info("registered event"+transaction);
        long utcNowTimestamp = timeService.getUtcNow();
        long currentEventsTime = transactionMadeEventHandler.getLogicalTime();
        if(currentEventsTime<lastUpdateLogicalTime.get()){ //avoid heavy calculation if it is already too late.
            return lastUpdateLogicalTime.get();
        }

        List<TimedEvent<Transaction>> eventHistorySnapshot = transactionMadeEventHandler.getHistory();
        long timeAfterUpdate = getLastEventTimeOr(eventHistorySnapshot, 0);
        if(timeAfterUpdate < lastUpdateLogicalTime.get()){ //avoid heavy calculation if it is already too late. Note, the
            return lastUpdateLogicalTime.get();
        }
        AggregateStatistics updatedStatistics = calculateStatistics(eventHistorySnapshot, utcNowTimestamp - MILISECONDS_IN_MINUTE, utcNowTimestamp);
        return tryUpdateStatistics(timeAfterUpdate, updatedStatistics);
    }

    /**
     * Tries to update the statistics with the newStatistics.
     * The update will only happen if the newUpdateLogical time is not stale (newUpdateLogicalTime > lastUpdatedTime).
     * This method is synchronized.
     * Start of a rant unimportant for the javadoc:
     * The use of the atomic long may come as surprizing here since this is the only method with sidefects ti this classes fields (in theory) and that means
     * that (in theory) the lastUpdateLogicalTime will never be modified during this methods execution. This is true (again in theory), and the need for atomic long
     * does not originate from here. We need an atomic long for the reads in the nonsync methods because as a 64 bit field, the update can take 2 instructions.
     * @return the last updated logical time. It will be the new UpdatedLogcal time if the update request was not stale or the old unchanged one if the update didn't happen
     * @param newUpdateLogicalTime the logical time for which the aggregatestatistic is calculated
     *                             @param newStatistics the new aggregate statistics that need to be set
     * */
    public synchronized long tryUpdateStatistics(long newUpdateLogicalTime, AggregateStatistics newStatistics) {
        long timeBforeUpdate = lastUpdateLogicalTime.get();
        if(timeBforeUpdate > newUpdateLogicalTime){
            return timeBforeUpdate;
        }

        if (lastUpdateLogicalTime.compareAndSet(timeBforeUpdate, newUpdateLogicalTime)) {
            this.statistics = newStatistics;
            LOG.info("Statistics updated for logical event time:"+newUpdateLogicalTime+" : "+newStatistics);
            return newUpdateLogicalTime;
        }

        return timeBforeUpdate;
    }


    /**
     * @param transactions  list of transaction events
     * @param fromTimestamp the begining utc timestamp of the transaction window of interest in miliseconds since utc time 0
     * @param toTimestamp   the end utc timestamp of the transaction window of interest in miliseconds since utc time 0
     * @return Aggregated statistics for the events of the colletion passed as a parameter whose transactions happened between fromTimestamp and toTimestamp (from and to included).
     * It uses a lot of fancy words whilst calculating it like: kahan summation/ compensation sumation.
     * This was (generously) provided by the java.util.stream.collectors implementation and simply means better floating point precission errors.
     * @throws IllegalArgumentException if toTimestamp is befor fromTimestamp
     */
    public AggregateStatistics calculateStatistics(Collection<TimedEvent<Transaction>> transactions, long fromTimestamp, long toTimestamp) throws IllegalArgumentException {
        LOG.info("Calculating statistigs from "+fromTimestamp +" to:"+ toTimestamp);
        if (toTimestamp < fromTimestamp) {
            throw new IllegalArgumentException("toTimestamp is befor fromTimestamp");
        }

        //Get snapshot of transactions in window of interest
        List<Double> recentTransactions = transactions
                .stream()
                .filter(x -> x.getEvent().getTimestamp() >= fromTimestamp)
                .filter(x -> x.getEvent().getTimestamp() <= toTimestamp)
                .map(TimedEvent::getEvent)
                .map(Transaction::getAmount)
                .collect(toList());

        //The following sums can be generated by at least one fewer call to the stream.collect(). (avg = sum/count) however it might cause some integer overflow issues if the list is really long
        Double avg = aggregate(recentTransactions, averagingDouble(x -> x));
        Double sum = aggregate(recentTransactions, summingDouble(x -> x));
        Double min = aggregate(recentTransactions, minBy(Double::compare)).orElse(0.0);
        Double max = aggregate(recentTransactions, maxBy(Double::compare)).orElse(0.0);
        Long count = aggregate(recentTransactions, counting());
        return new AggregateStatistics(sum, avg, max, min, count);
    }

     private <TaggregateRez, Telems> TaggregateRez aggregate(List<Telems> lst, Collector<Telems, ?, TaggregateRez> collector) {
        return lst.stream().collect(collector);
    }

    /**
     * A method to find the latest event in a list. Current implementation is simly get last element of the list since the event list is being ordered by logical event time.
     * Start of rant that is not javadoc:
     * It should be changed with getting the max time if needed in the future
     * @return The logical time of the last element or the default value if such doesn't exist or the list is null
     * @param eventHistorySnapshot the source list with events
     *                             @param defaultValue return value if the list is null or empty
     * */
    public long getLastEventTimeOr(List<TimedEvent<Transaction>> eventHistorySnapshot, long defaultValue) {
        if (eventHistorySnapshot == null || eventHistorySnapshot.isEmpty()) {
            return defaultValue;
        }

        return eventHistorySnapshot.get(eventHistorySnapshot.size() - 1).getLogicalTime();
    }
}
