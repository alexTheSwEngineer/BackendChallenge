package com.n26.atrposki.transactions;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.domain.Transaction;
import com.n26.atrposki.domain.TransactionMadeEventHandler;
import com.n26.atrposki.statistics.StatisticsService;
import com.n26.atrposki.utils.time.ITimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import static com.n26.atrposki.utils.time.ITimeService.MILISECONDS_IN_MINUTE;

@Service
public class TransactionsService {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionsService.class);

    TransactionMadeEventHandler transactionMadeEventHandler;
    ITimeService timeService;

    @Autowired
    public TransactionsService(TransactionMadeEventHandler transactionMadeEventHandler, ITimeService timeService) {
        this.transactionMadeEventHandler = transactionMadeEventHandler;
        this.timeService = timeService;
    }

    public boolean createTransaction(TransactionDTO transaction) {
        LOG.info("creating transaction"+transaction);
        long now = timeService.getUtcNow();
        Transaction transactionEvent = new Transaction(transaction.getAmount(),transaction.getTimestamp());
        transactionMadeEventHandler.publish(transactionEvent);
        return transactionEvent.getTimestamp() > now - MILISECONDS_IN_MINUTE;
    }
}
