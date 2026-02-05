package com.jeleniasty.budgetaggregator.service.imports;

import com.jeleniasty.budgetaggregator.model.TransactionDto;
import com.jeleniasty.budgetaggregator.model.events.ImportStatusUpdateEvent;
import com.jeleniasty.budgetaggregator.model.imports.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.jeleniasty.budgetaggregator.model.TransactionType.CREDIT;
import static com.jeleniasty.budgetaggregator.model.imports.ImportStatus.COMPLETED;
import static com.jeleniasty.budgetaggregator.model.imports.ImportStatus.FAILED;
import static com.jeleniasty.budgetaggregator.model.imports.ImportStatus.PARTIALLY_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionImporterTest {

    private ApplicationEventPublisher eventPublisher;
    private TransactionValidator validator;
    private TransactionDataService dataService;
    private TransactionImporter importer;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        validator = mock(TransactionValidator.class);
        dataService = mock(TransactionDataService.class);
        importer = new TransactionImporter(eventPublisher, validator, dataService);
    }

    @Test
    void shouldPublishCompleted_whenAllTransactionsSaved() {
        String importId = "imp1";
        InputStream input = new ByteArrayInputStream("dummy".getBytes());

        List<TransactionDto> validTransactions = prepareTransactions();

        ValidationResult validationResult = new ValidationResult(3, validTransactions, new ArrayList<>());
        when(validator.validateAndMap(input)).thenReturn(validationResult);

        importer.importTransactions(importId, input);

        verify(dataService).saveBatch(validTransactions, importId);

        ArgumentCaptor<ImportStatusUpdateEvent> captor = ArgumentCaptor.forClass(ImportStatusUpdateEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ImportStatusUpdateEvent event = captor.getValue();
        assertThat(event.id()).isEqualTo(importId);
        assertThat(event.status()).isEqualTo(COMPLETED);
        assertThat(event.totalRows()).isEqualTo(3);
        assertThat(event.successfulRows()).isEqualTo(3);
        assertThat(event.errors()).isEmpty();
    }

    @Test
    void shouldPublishFailedCompleted_whenSomeBatchesFail() {
        String importId = "imp2";
        InputStream input = new ByteArrayInputStream("dummy".getBytes());

        List<TransactionDto> validTransactions = prepareTransactions();

        ValidationResult validationResult = new ValidationResult(3, validTransactions, new ArrayList<>());
        when(validator.validateAndMap(input)).thenReturn(validationResult);

        doThrow(new RuntimeException("Batch failed"))
                .when(dataService).saveBatch(validTransactions, importId);

        importer.importTransactions(importId, input);

        ArgumentCaptor<ImportStatusUpdateEvent> captor = ArgumentCaptor.forClass(ImportStatusUpdateEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ImportStatusUpdateEvent event = captor.getValue();
        assertThat(event.status()).isEqualTo(FAILED);
        assertThat(event.errors()).contains("Batch failed");
    }

    @Test
    void shouldPublishPartiallyCompleted_whenSomeBatchesSucceed() {
        String importId = "impPartial";
        InputStream input = new ByteArrayInputStream("dummy".getBytes());

        int batchSize = 500;
        int totalTransactions = 600;
        List<TransactionDto> transactions = new ArrayList<>();
        for (int i = 0; i < totalTransactions; i++) {
            transactions.add(new TransactionDto("bank" + i, "ref" + i, "IBAN" + i, Instant.now(), "EUR", "Category" + i, CREDIT, BigDecimal.valueOf(100 + i)));
        }

        ValidationResult validationResult = new ValidationResult(
                totalTransactions,
                transactions,
                new ArrayList<>()
        );
        when(validator.validateAndMap(input)).thenReturn(validationResult);

        doNothing().when(dataService).saveBatch(transactions.subList(0, batchSize), importId);
        doThrow(new RuntimeException("Batch failed")).when(dataService).saveBatch(transactions.subList(batchSize, totalTransactions), importId);

        importer.importTransactions(importId, input);

        ArgumentCaptor<ImportStatusUpdateEvent> captor = ArgumentCaptor.forClass(ImportStatusUpdateEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ImportStatusUpdateEvent event = captor.getValue();

        assertThat(event.status()).isEqualTo(PARTIALLY_COMPLETED);
        assertThat(event.totalRows()).isEqualTo(totalTransactions);
        assertThat(event.successfulRows()).isEqualTo(batchSize);
        assertThat(event.errors()).containsExactly("Batch failed");
    }

    @Test
    void shouldPublishFailed_whenNoValidTransactions() {
        String importId = "imp3";
        InputStream input = new ByteArrayInputStream("dummy".getBytes());

        ValidationResult validationResult = new ValidationResult(0, new ArrayList<>(), new ArrayList<>());
        when(validator.validateAndMap(input)).thenReturn(validationResult);

        importer.importTransactions(importId, input);

        ArgumentCaptor<ImportStatusUpdateEvent> captor = ArgumentCaptor.forClass(ImportStatusUpdateEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ImportStatusUpdateEvent event = captor.getValue();
        assertThat(event.status()).isEqualTo(FAILED);
    }

    @Test
    void shouldHandleCriticalException_andPublishFailed() {
        String importId = "imp4";
        InputStream input = new ByteArrayInputStream("dummy".getBytes());

        List<TransactionDto> validTransactions = List.of(new TransactionDto("bank1", "ref1", "IBAN1", Instant.now(), "EUR", "Food", CREDIT, BigDecimal.valueOf(100)));
        ValidationResult validationResult = new ValidationResult(1, validTransactions, new ArrayList<>());
        when(validator.validateAndMap(input)).thenReturn(validationResult);

        doThrow(new RuntimeException("Critical")).when(dataService).saveBatch(anyList(), eq(importId));

        importer.importTransactions(importId, input);

        ArgumentCaptor<ImportStatusUpdateEvent> captor = ArgumentCaptor.forClass(ImportStatusUpdateEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ImportStatusUpdateEvent event = captor.getValue();
        assertThat(event.status()).isEqualTo(FAILED);
        assertThat(event.errors()).contains("Critical");
    }

    @Test
    void shouldSplitIntoBatches_whenLargeNumberOfTransactions() {
        String importId = "imp5";
        InputStream input = new ByteArrayInputStream("dummy".getBytes());

        int total = 1200;
        List<TransactionDto> transactions = new ArrayList<>();
        for (int i = 0; i < total; i++)
            transactions.add(new TransactionDto("bank1", "ref1", "IBAN1", Instant.now(), "EUR", "Food", CREDIT, BigDecimal.valueOf(100)));

        ValidationResult validationResult = new ValidationResult(total, transactions, new ArrayList<>());
        when(validator.validateAndMap(input)).thenReturn(validationResult);

        importer.importTransactions(importId, input);

        verify(dataService, times(3)).saveBatch(anyList(), eq(importId));

        ArgumentCaptor<ImportStatusUpdateEvent> captor = ArgumentCaptor.forClass(ImportStatusUpdateEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ImportStatusUpdateEvent event = captor.getValue();
        assertThat(event.successfulRows()).isEqualTo(total);
        assertThat(event.status()).isEqualTo(COMPLETED);
    }


    private static List<TransactionDto> prepareTransactions() {
        return List.of(
                new TransactionDto("bank1", "ref1", "IBAN1", Instant.now(), "EUR", "Food", CREDIT, BigDecimal.valueOf(100)),
                new TransactionDto("bank2", "ref2", "IBAN2", Instant.now(), "USD", "Travel", CREDIT, BigDecimal.valueOf(200)),
                new TransactionDto("bank3", "ref3", "IBAN3", Instant.now(), "PLN", "Shopping", CREDIT, BigDecimal.valueOf(300))
        );
    }
}
