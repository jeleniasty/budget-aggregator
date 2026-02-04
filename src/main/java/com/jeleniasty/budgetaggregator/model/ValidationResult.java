package com.jeleniasty.budgetaggregator.model;

import java.util.List;

public record ValidationResult(int totalRows, List<TransactionDto> validTransactions, List<String> errors) {
}
