package com.jeleniasty.budgetaggregator.model.aggregation;

import java.math.BigDecimal;

public record AggregationSummary(
        String category,
        String iban,
        String month,
        String currency,
        BigDecimal inflow,
        BigDecimal outflow,
        BigDecimal balance,
        long transactionCount
) {}
