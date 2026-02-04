package com.jeleniasty.budgetaggregator.model.aggregation;

public record AggregationParameters(
        String category,
        String iban,
        String month
) {
}
