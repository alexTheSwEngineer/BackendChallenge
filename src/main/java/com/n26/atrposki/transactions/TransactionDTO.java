package com.n26.atrposki.transactions;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
public class TransactionDTO {
    double amount;
    long timestamp;

    public TransactionDTO(){}

    public TransactionDTO(double amount, long timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

}
