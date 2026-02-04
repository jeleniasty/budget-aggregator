package com.jeleniasty.budgetaggregator.model.aggregation;

import io.swagger.v3.oas.annotations.media.Schema;

public record AggregationParameters(
        @Schema(description = "Transaction category to filter (optional)", example = "Groceries")
        String category,
        @Schema(description = "IBAN to filter (optional)", example = "DE89370400440532013000")
        String iban,
        @Schema(description = "Month to filter (optional, format YYYY-MM)", example = "2026-03", pattern = "\\d{4}-\\d{2}")
        String month
) { }
