package com.jeleniasty.budgetaggregator.model.aggregation;

import java.math.BigDecimal;

public record AggregationRaw(
        String currency,
        String category,
        String iban,
        String month,
        BigDecimal inflow,
        BigDecimal outflow,
        BigDecimal balance,
        long transactionCount
) {}
