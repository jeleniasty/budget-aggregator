package com.jeleniasty.budgetaggregator.model;

public enum TransactionType {
    DEBIT,
    CREDIT;

    public static TransactionType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }

        try {
            return TransactionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction type: " + value + ". Expected DEBIT or CREDIT.");
        }
    }
}