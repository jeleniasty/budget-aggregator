package com.jeleniasty.budgetaggregator.service.imports;

import com.jeleniasty.budgetaggregator.exception.ImportMetadataException;
import com.jeleniasty.budgetaggregator.model.events.ImportStatusUpdateEvent;
import com.jeleniasty.budgetaggregator.model.imports.*;
import com.jeleniasty.budgetaggregator.persistence.importjob.ImportMetadata;
import com.jeleniasty.budgetaggregator.persistence.importjob.ImportMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    private ImportMetadata buildMetadata(String fileName, ImportStatus status, String id) {
        ImportMetadata metadata = new ImportMetadata(fileName, status);
        metadata.setId(id);
        return metadata;
    }

    private MultipartFile mockFile(String name, byte[] content) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(name);
        when(file.getBytes()).thenReturn(content);
        return file;
    }

    @Nested
    class ImportFileTests {

        @Test
        void shouldSaveMetadataAndPublishEvent_whenFileIsValid() throws IOException {
            MultipartFile file = mockFile("test.csv", "content".getBytes());

            when(repository.save(any())).thenAnswer(invocation -> {
                ImportMetadata metadata = invocation.getArgument(0);
                metadata.setId("123");
                return metadata;
            });

            ImportResponse response = service.importFile(file);

            assertThat(response.id()).isEqualTo("123");
            assertThat(response.fileName()).isEqualTo("test.csv");
            assertThat(response.status()).isEqualTo(PROCESSING);

            ArgumentCaptor<ImportMetadata> savedCaptor = ArgumentCaptor.forClass(ImportMetadata.class);
            verify(repository, times(1)).save(savedCaptor.capture());
            ImportMetadata saved = savedCaptor.getValue();
            assertThat(saved.getFileName()).isEqualTo("test.csv");
            assertThat(saved.getStatus()).isEqualTo(PROCESSING);

            ArgumentCaptor<ImportCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ImportCreatedEvent.class);
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
            ImportCreatedEvent published = eventCaptor.getValue();
            assertThat(published.importId()).isEqualTo(saved.getId());
            assertThat(published.content()).isEqualTo("content".getBytes());
        }

        @Test
        void shouldSetFailedStatus_whenIOExceptionOccurs() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("fail.csv");
            when(file.getBytes()).thenThrow(new IOException("IO Error"));

            ImportMetadata metadata = new ImportMetadata("fail.csv", PROCESSING);
            when(repository.save(any())).thenReturn(metadata);

            ImportResponse response = service.importFile(file);

            assertThat(response.fileName()).isEqualTo("fail.csv");
            assertThat(response.status()).isEqualTo(ImportStatus.FAILED);

            verify(repository, times(2)).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    class HandleStatusUpdateEventTests {

        @Test
        void shouldUpdateMetadata_whenMetadataExists() {
            String id = "123";
            ImportMetadata metadata = buildMetadata("file.csv", PROCESSING, id);
            when(repository.findById(id)).thenReturn(Optional.of(metadata));

            ImportStatusUpdateEvent event = new ImportStatusUpdateEvent(
                    id, ImportStatus.COMPLETED, 100, 95, List.of("err1", "err2")
            );

            service.handleImportStatusUpdateEvent(event);

            assertThat(metadata.getStatus()).isEqualTo(ImportStatus.COMPLETED);
            assertThat(metadata.getTotalRows()).isEqualTo(100);
            assertThat(metadata.getSuccessfulRows()).isEqualTo(95);
            assertThat(metadata.getErrors()).containsExactly("err1", "err2");

            verify(repository, times(1)).save(metadata);
        }

        @Test
        void shouldThrow_whenMetadataNotFound() {
            String id = "missing";
            when(repository.findById(id)).thenReturn(Optional.empty());

            ImportStatusUpdateEvent event = new ImportStatusUpdateEvent(
                    id, ImportStatus.COMPLETED, 10, 10, List.of()
            );

            assertThatThrownBy(() -> service.handleImportStatusUpdateEvent(event))
                    .isInstanceOf(ImportMetadataException.class)
                    .hasMessageContaining(id);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class GetImportDetailsTests {

        @Test
        void shouldReturnDto_whenMetadataExists() {
            String id = "123";
            ImportMetadata metadata = buildMetadata("file.csv", PROCESSING, id);
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
        void shouldThrow_whenMetadataNotFound() {
            String id = "missing";
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getImportDetails(id))
                    .isInstanceOf(ImportMetadataException.class)
                    .hasMessageContaining(id);
        }
    }
}
