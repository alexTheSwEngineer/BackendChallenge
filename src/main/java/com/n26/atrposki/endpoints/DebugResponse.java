package com.n26.atrposki.endpoints;
/*
 * @author aleksandartrposki@gmail.com
 * @since 06.05.18
 *
 *
 */

import com.n26.atrposki.domain.AggregateStatistics;
import com.n26.atrposki.transactions.TransactionDTO;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class DebugResponse {
    TransactionDTO transaction;
    HttpStatus status;
    AggregateStatistics aggregateStatistics;

    public DebugResponse(){}
    public DebugResponse(TransactionDTO transaction, HttpStatus status, AggregateStatistics aggregateStatistics) {
        this.transaction = transaction;
        this.status = status;
        this.aggregateStatistics = aggregateStatistics;
    }
}
