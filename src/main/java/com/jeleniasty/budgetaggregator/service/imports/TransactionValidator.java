package com.jeleniasty.budgetaggregator.service.imports;

import com.jeleniasty.budgetaggregator.model.TransactionDto;
import com.jeleniasty.budgetaggregator.model.TransactionType;
import com.jeleniasty.budgetaggregator.model.imports.ValidationResult;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class TransactionValidator {

    private static final DateTimeFormatter FALLBACK_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String IBAN_REGEX = "[A-Z]{2}\\d{2}[A-Z0-9]{1,30}";

    public ValidationResult validateAndMap(InputStream inputStream) {
        if (inputStream == null) {
            return new ValidationResult(0, List.of(), List.of("Source stream is null"));
        }

        List<TransactionDto> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;

        try (var reader = CsvReader.builder().ofNamedCsvRecord(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            for (NamedCsvRecord row : reader) {
                totalRows++;
                long lineNum = row.getStartingLineNumber();
                List<String> rowErrors = new ArrayList<>();

                String bankId = row.getField("Bank");
                String referenceId = row.getField("Reference number");
                String iban = row.getField("IBAN");
                String dateStr = row.getField("Date");
                String currency = row.getField("Currency");
                String category = row.getField("Category");
                String transactionTypeStr = row.getField("Transaction type");
                String amountStr = row.getField("Amount");

                validateBankId(bankId, rowErrors);
                validateReferenceId(referenceId, rowErrors);
                validateIban(iban, rowErrors);
                Instant transactionDate = validateTransactionDate(dateStr, rowErrors);
                validateCurrency(currency, rowErrors);
                validateCategory(category, rowErrors);
                BigDecimal amount = validateAmount(amountStr, rowErrors);
                TransactionType transactionType = validateTransactionType(transactionTypeStr, rowErrors);

                if (rowErrors.isEmpty()) {
                    validRows.add(new TransactionDto(bankId, referenceId, iban, transactionDate, currency, category, transactionType, amount));
                } else {
                    errors.add("Line " + lineNum + ": " + String.join(", ", rowErrors));
                }
            }
        } catch (IOException e) {
            log.error("IOException", e);
            errors.add("Critical I/O error during parsing: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected exception", e);
            errors.add("Unexpected error: " + e.getMessage());
        }

        return new ValidationResult(totalRows, validRows, errors);
    }


    private BigDecimal validateAmount(String amountStr, List<String> rowErrors) {
        BigDecimal amount = null;
        try {
            amount = new BigDecimal(amountStr.replace(",", "."));
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                rowErrors.add("Amount cannot be negative");
            }
        } catch (NumberFormatException e) {
            rowErrors.add("Invalid amount");
        }
        return amount;
    }

    private void validateCategory(String category, List<String> rowErrors) {
        if (category.isBlank()) {
            rowErrors.add("Category is empty");
        }
    }

    private void validateCurrency(String currency, List<String> rowErrors) {
        if (currency.isBlank()) {
            rowErrors.add("Currency is empty");
        }
    }

    private Instant validateTransactionDate(String dateStr, List<String> rowErrors) {
        Instant transactionDate = null;
        try {
            transactionDate = parseDate(dateStr);
        } catch (DateTimeParseException e) {
            rowErrors.add("Invalid date");
        }
        return transactionDate;
    }

    private void validateIban(String iban, List<String> rowErrors) {
        if (iban.isBlank() || !iban.matches(IBAN_REGEX)) {
            rowErrors.add("Invalid IBAN");
        }
    }

    private void validateBankId(String bankId, List<String> rowErrors) {
        if (bankId.isBlank()) {
            rowErrors.add("Bank is empty");
        }
    }

    private void validateReferenceId(String referenceId, List<String> rowErrors) {
        if (referenceId.isBlank()) {
            rowErrors.add("Reference number is empty");
        }
    }

    private TransactionType validateTransactionType(String transactionType, List<String> rowErrors) {
        if (transactionType.isBlank()) {
            rowErrors.add("Transaction type is empty");
            return null;
        }

        try {
            return TransactionType.valueOf(transactionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            rowErrors.add("Invalid transaction type: " + transactionType);
            return null;
        }
    }

    private Instant parseDate(String dateStr) {
        try {
            return Instant.parse(dateStr);
        } catch (DateTimeParseException e1) {
            try {
                return Instant.from(FALLBACK_DATE_FORMAT.parse(dateStr));
            } catch (DateTimeParseException e2) {
                throw new DateTimeParseException("Cannot parse date: " + dateStr, dateStr, 0);
            }
        }
    }
}
