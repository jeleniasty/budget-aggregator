package com.jeleniasty.budgetaggregator.service.imports;

import com.jeleniasty.budgetaggregator.model.ImportStatus;
import com.jeleniasty.budgetaggregator.model.TransactionDto;
import com.jeleniasty.budgetaggregator.model.events.ImportStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jeleniasty.budgetaggregator.model.ImportStatus.COMPLETED;
import static com.jeleniasty.budgetaggregator.model.ImportStatus.FAILED;
import static com.jeleniasty.budgetaggregator.model.ImportStatus.PARTIALLY_COMPLETED;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionImporter {

    private static final int BATCH_SIZE = 500;

    private final ApplicationEventPublisher eventPublisher;
    private final TransactionValidator validator;
    private final TransactionDataService dataService;

    public void importTransactions(String importId, InputStream inputStream) {
        log.info("Transactions import {} started...", importId);
        var validationResult = validator.validateAndMap(inputStream);
        log.info("Transactions import {} validation completed. Total rows: {}. Errors: {}", importId, validationResult.totalRows(), validationResult.errors().size());
        var validTransactions = validationResult.validTransactions();

        var savedCount = new AtomicInteger(0);
        var errors = validationResult.errors();
        try {
            for (int i = 0; i < validTransactions.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, validTransactions.size());
                List<TransactionDto> batch = validTransactions.subList(i, end);

                try {
                    dataService.saveBatch(batch, importId);
                    savedCount.addAndGet(batch.size());
                    log.info("Batch {}-{} saved successfully for import {}", i, end, importId);
                } catch (Exception exception) {
                    log.error("Failed batch {}-{} for import {}: {}",
                            i, end, importId, exception.getMessage());
                    errors.add(exception.getMessage());
                }
            }

            eventPublisher.publishEvent(new ImportStatusUpdateEvent(importId, calculateStatus(validTransactions.size(), savedCount.intValue()), validationResult.totalRows(), savedCount.get(), validationResult.errors()));
            log.info("Transactions import {} completed...", importId);

        } catch (Exception criticalEx) {
            log.error("Critical failure during import {}", importId, criticalEx);
            eventPublisher.publishEvent(new ImportStatusUpdateEvent(importId, FAILED, validationResult.totalRows(), savedCount.get(), validationResult.errors()));
        }
    }

    private ImportStatus calculateStatus(int total, int saved) {
        if (total == 0 || saved == 0) return FAILED;
        if (saved == total) return COMPLETED;
        if (saved > 0) return PARTIALLY_COMPLETED;
        return FAILED;
    }
}
