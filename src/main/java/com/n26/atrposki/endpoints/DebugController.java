package com.n26.atrposki.endpoints;
/*
 * @author aleksandartrposki@gmail.com
 * @since 06.05.18
 *
 *
 */

import com.n26.atrposki.domain.AggregateStatistics;
import com.n26.atrposki.domain.Transaction;
import com.n26.atrposki.statistics.StatisticsService;
import com.n26.atrposki.transactions.TransactionDTO;
import com.n26.atrposki.transactions.TransactionsService;
import com.n26.atrposki.utils.time.ITimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.n26.atrposki.utils.time.ITimeService.*;

@RestController
@RequestMapping("/debug")
public class DebugController {
    ITimeService timeService;
    TransactionsService transactionsService;
    StatisticsService statisticsService;

    @Autowired
    public DebugController(ITimeService timeService, TransactionsService transactionsService, StatisticsService statisticsService) {
        this.timeService = timeService;
        this.transactionsService = transactionsService;
        this.statisticsService = statisticsService;
    }

    @RequestMapping("/createrandom")
    public DebugResponse createRandom(){
        TransactionDTO t = new TransactionDTO();
        t.setAmount(Math.random()*1000);
        long randomTime = new Double(MILISECONDS_IN_MINUTE*Math.random()).longValue();
        t.setTimestamp(timeService.getUtcNow()-randomTime);
        boolean status = transactionsService.createTransaction(t);
        AggregateStatistics statistics = statisticsService.getStatistics();
        return new DebugResponse(t,status? HttpStatus.CREATED:HttpStatus.NO_CONTENT,statistics);
    }

    @RequestMapping("/time")
    public long getTime(){
        return timeService.getUtcNow();
    }
}
