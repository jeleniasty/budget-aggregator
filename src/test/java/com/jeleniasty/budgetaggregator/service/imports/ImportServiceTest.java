package com.jeleniasty.budgetaggregator.service.imports;

import com.jeleniasty.budgetaggregator.exception.ImportMetadataException;
import com.jeleniasty.budgetaggregator.model.events.ImportStatusUpdateEvent;
import com.jeleniasty.budgetaggregator.model.imports.*;
import com.jeleniasty.budgetaggregator.persistence.importjob.ImportMetadata;
import com.jeleniasty.budgetaggregator.persistence.importjob.ImportMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.jeleniasty.budgetaggregator.model.imports.ImportStatus.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ImportServiceTest {

    private ImportMetadataRepository repository;
    private ApplicationEventPublisher eventPublisher;
    private ImportService service;

    @BeforeEach
    void setUp() {
        repository = mock(ImportMetadataRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ImportService(repository, eventPublisher);
    }

    //importFile tests
    @Test
    void importFile_shouldSaveMetadataAndPublishEvent_whenFileIsValid() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.csv");
        when(file.getBytes()).thenReturn("content".getBytes());
        when(repository.save(any())).thenAnswer(invocation -> {
            ImportMetadata metadata = invocation.getArgument(0);
            metadata.setId("123");
            return metadata;
        });

        ImportResponse response = service.importFile(file);

        assertThat(response.fileName()).isEqualTo("test.csv");
        assertThat(response.status()).isEqualTo(PROCESSING);
        assertThat(response.id()).isNotNull();

        ArgumentCaptor<ImportMetadata> saved = ArgumentCaptor.forClass(ImportMetadata.class);
        verify(repository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getFileName()).isEqualTo("test.csv");
        assertThat(saved.getValue().getStatus()).isEqualTo(PROCESSING);

        ArgumentCaptor<ImportCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ImportCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().importId()).isEqualTo(saved.getValue().getId());
        assertThat(eventCaptor.getValue().content()).isEqualTo("content".getBytes());
    }

    @Test
    void importFile_shouldSetFailedStatus_whenIOExceptionOccurs() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("fail.csv");
        when(file.getBytes()).thenThrow(new IOException("IO Error"));

        ImportMetadata savedMetadata = new ImportMetadata("fail.csv", PROCESSING);
        when(repository.save(any())).thenReturn(savedMetadata);

        ImportResponse response = service.importFile(file);

        assertThat(response.fileName()).isEqualTo("fail.csv");
        assertThat(response.status()).isEqualTo(ImportStatus.FAILED);

        verify(repository, times(2)).save(any());
        verifyNoInteractions(eventPublisher);
    }

    //handleImportStatusUpdateEvent tests
    @Test
    void handleImportStatusUpdateEvent_shouldUpdateMetadata() {
        String id = "123";
        ImportMetadata metadata = new ImportMetadata("file.csv", PROCESSING);
        when(repository.findById(id)).thenReturn(Optional.of(metadata));

        ImportStatusUpdateEvent event = new ImportStatusUpdateEvent(
                id,
                ImportStatus.COMPLETED,
                100,
                95,
                List.of("error1", "error2")
        );

        service.handleImportStatusUpdateEvent(event);

        assertThat(metadata.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(metadata.getTotalRows()).isEqualTo(100);
        assertThat(metadata.getSuccessfulRows()).isEqualTo(95);
        assertThat(metadata.getErrors()).containsExactly("error1", "error2");

        verify(repository).save(metadata);
    }

    @Test
    void handleImportStatusUpdateEvent_shouldThrow_whenMetadataNotFound() {
        String id = "nonexistent";
        when(repository.findById(id)).thenReturn(Optional.empty());

        ImportStatusUpdateEvent event = new ImportStatusUpdateEvent(
                id, ImportStatus.COMPLETED, 10, 10, List.of()
        );

        assertThatThrownBy(() -> service.handleImportStatusUpdateEvent(event))
                .isInstanceOf(ImportMetadataException.class)
                .hasMessageContaining(id);

        verify(repository, never()).save(any());
    }

    //getImportDetails tests
    @Test
    void getImportDetails_shouldReturnDto_whenMetadataExists() {
        String id = "123";
        ImportMetadata metadata = new ImportMetadata("file.csv", PROCESSING);
        metadata.setId(id);
        metadata.setTotalRows(50);
        metadata.setSuccessfulRows(45);
        metadata.setErrors(List.of("err1"));
        when(repository.findById(id)).thenReturn(Optional.of(metadata));

        ImportDetailsDto dto = service.getImportDetails(id);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.fileName()).isEqualTo("file.csv");
        assertThat(dto.status()).isEqualTo(PROCESSING);
        assertThat(dto.totalRows()).isEqualTo(50);
        assertThat(dto.successfulRows()).isEqualTo(45);
        assertThat(dto.errors()).containsExactly("err1");
    }

    @Test
    void getImportDetails_shouldThrow_whenMetadataNotFound() {
        String id = "missing";
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getImportDetails(id))
                .isInstanceOf(ImportMetadataException.class)
                .hasMessageContaining(id);
    }
}
