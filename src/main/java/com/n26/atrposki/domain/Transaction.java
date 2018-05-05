package com.n26.atrposki.domain;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
public class Transaction {
    private final double amount;
    private final long timestamp;
}
