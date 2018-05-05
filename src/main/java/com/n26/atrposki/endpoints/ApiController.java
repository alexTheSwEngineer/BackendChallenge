package com.n26.atrposki.endpoints;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.domain.AggregateStatistics;
import com.n26.atrposki.domain.Transaction;
import com.n26.atrposki.statistics.StatisticsService;
import com.n26.atrposki.transactions.TransactionDTO;
import com.n26.atrposki.transactions.TransactionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Endpoints for the api.
 * Lacks any serious exception handling.
 * */
@RestController
@RequestMapping("/api")
public class ApiController {

    private StatisticsService statisticsService;
    private TransactionsService transactionService;

    @Autowired
    public ApiController(StatisticsService statisticsService, TransactionsService transactionService) {
        this.statisticsService = statisticsService;
        this.transactionService = transactionService;
    }

    @RequestMapping(method = GET, value = "/statistics/latest")
    public AggregateStatistics getStatisticsForLastMin() {
        return statisticsService.getStatistics();
    }

    @RequestMapping(method = POST, value = "/transactions/")
    public ResponseEntity createTransaction(@RequestBody TransactionDTO transaction) {
        boolean isInWindowOfInterest = transactionService.createTransaction(transaction);
        if(isInWindowOfInterest){
            return status(CREATED).body(null);
        }else{
            return status(NO_CONTENT).body(null);
        }
    }
}
