package com.jeleniasty.budgetaggregator.service.imports;

import com.jeleniasty.budgetaggregator.model.imports.ImportCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.ByteArrayInputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportListener {
    private final TransactionImporter importer;

    @Async("importExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImportCreated(ImportCreatedEvent event) {

        log.info("Starting async import {}", event.importId());
        importer.importTransactions(
                event.importId(),
                new ByteArrayInputStream(event.content())
        );
    }
}
