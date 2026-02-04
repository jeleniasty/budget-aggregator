package com.jeleniasty.budgetaggregator.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDto(String bankId, String referenceId, String iban, Instant transactionDate, String currency,
                             String category, TransactionType transactionType, BigDecimal amount) {
}
