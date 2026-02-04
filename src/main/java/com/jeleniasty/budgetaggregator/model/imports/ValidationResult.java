package com.jeleniasty.budgetaggregator.model.imports;

import com.jeleniasty.budgetaggregator.model.TransactionDto;

import java.util.List;

public record ValidationResult(int totalRows, List<TransactionDto> validTransactions, List<String> errors) {
}
