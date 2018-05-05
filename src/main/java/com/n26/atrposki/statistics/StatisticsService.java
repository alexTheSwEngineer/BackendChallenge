package com.n26.atrposki.statistics;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.utils.events.TimedEvent;
import com.n26.atrposki.utils.time.ITimeService;
import com.n26.atrposki.domain.AggregateStatistics;
import com.n26.atrposki.domain.Transaction;
import com.n26.atrposki.domain.TransactionMadeEventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
 * b) There will be much more reads then writes. (if nothing happenes in 60 seconds, no updates will occur).
 *
 * A simple way to fight this is to:
 * a) Keep only a sliding widnow of relevant events having O(1) update sliding window time and O(n) memory and time complexity
 * b) Schedule transaction expired events, or regular updates in a resonable interval.
 *
 * If breaking the O(1) memory constrain is a possibility we can keep min/max predecessors so each insert/update/read will be O(1) however we will need O(n) memory.
 * Where N is not from the begining of time, but the number of transactions in a 60  sec window.
 *
 * */
@Service
public class StatisticsService {
    private ITimeService timeService;
    private TransactionMadeEventHandler transactionMadeEventHandler;
    private volatile AggregateStatistics statistics;
    private AtomicLong lastUpdateLogicalTime;

    @Autowired
    public StatisticsService(ITimeService timeService, TransactionMadeEventHandler transactionMadeEventHandler) {
        this.timeService = timeService;
        this.transactionMadeEventHandler = transactionMadeEventHandler;
        lastUpdateLogicalTime = new AtomicLong(0);
        statistics = new AggregateStatistics();
        transactionMadeEventHandler.subscribe(this::onTransactionMade);
    }

    public AggregateStatistics getStatistics() {
        return statistics;
    }

    /**
     * This method is envoked on every transaction event an updates the AggregationStatistics if needed.
     * This method deals with concurency of events and the possibility of a mixed order of events
     * by only doing an actuall update if the event logical time is larger then the last update logical time (which should be unique).
     * More over it does that in a syncronized manner curtecy of the atomic integer implementation.
     * All that being said, this is not really needed in this current scenario. It doesn't matter if the events come in a diferent order
     * since the statistics is (quite inefficiently) recalculated every time, its only here in this way to showcase the concept. The underlying implementation of getAndUpdate doesn't make it any better.
     * @param transaction a timed event
     * @return the new logical time for the last update of the AggregateStatistics value.
     * */
    public long onTransactionMade(TimedEvent<Transaction> transaction) {
        lastUpdateLogicalTime.getAndUpdate(now -> {
            if (now > transaction.getLogicalTime()) {
                return now;
            }

            this.statistics = calculateStatisticsFromLastMinute();
            return transaction.getLogicalTime();
        });
        return lastUpdateLogicalTime.get();
    }

    /**
     * @return A new statistics for the last 60 seconds generated from the latest transaction history.
     * This method gives back "eventualy" consistent results.
     * However, this is crawling through the history since the begining of time, which is less then optimal.
     * A better solution would be this service to keep track only of a sliding window of the whole history and aggregate that.
     * */
    public AggregateStatistics calculateStatisticsFromLastMinute() {
        long utcNow = timeService.getUtcNow();
        long utcBefore1min = utcNow - MILISECONDS_IN_MINUTE;
        List<Double> recentTransactions = transactionMadeEventHandler.getHistory()
                .stream()
                .filter(x -> x.getEvent().getTimestamp() <= utcNow)
                .filter(x -> x.getEvent().getTimestamp() >= utcBefore1min)
                .map(TimedEvent::getEvent)
                .map(Transaction::getAmount)
                .collect(Collectors.toList());

        Double avg = aggregate(recentTransactions, averagingDouble(x -> x));
        Double sum = aggregate(recentTransactions, summingDouble(x -> x));
        Double min = aggregate(recentTransactions, minBy(Double::compare)).orElse(0.0);
        Double max = aggregate(recentTransactions, maxBy(Double::compare)).orElse(0.0);
        Long count = aggregate(recentTransactions, counting());
        return new AggregateStatistics(sum, avg, max, min, count);
    }


    private <T> T aggregate(List<Double> lst, Collector<Double, ?, T> collector) {
        return lst.stream().collect(collector);
    }
}
