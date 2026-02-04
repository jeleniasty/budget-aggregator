package com.jeleniasty.budgetaggregator.service.imports;

import com.jeleniasty.budgetaggregator.exception.ImportMetadataException;
import com.jeleniasty.budgetaggregator.model.events.ImportStatusUpdateEvent;
import com.jeleniasty.budgetaggregator.model.imports.ImportCreatedEvent;
import com.jeleniasty.budgetaggregator.model.imports.ImportDetailsDto;
import com.jeleniasty.budgetaggregator.model.imports.ImportResponse;
import com.jeleniasty.budgetaggregator.model.imports.ImportStatus;
import com.jeleniasty.budgetaggregator.persistence.importjob.ImportMetadata;
import com.jeleniasty.budgetaggregator.persistence.importjob.ImportMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.jeleniasty.budgetaggregator.model.imports.ImportStatus.PROCESSING;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final ImportMetadataRepository importMetadataRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ImportResponse importFile(MultipartFile file) {
        var importMetadata = importMetadataRepository.save(new ImportMetadata(file.getOriginalFilename(), PROCESSING));

        final byte[] content;

        try {
            content = file.getBytes();
        } catch (IOException e) {
            importMetadata.setStatus(ImportStatus.FAILED);
            importMetadataRepository.save(importMetadata);
            return new ImportResponse(importMetadata.getId(), importMetadata.getFileName(), importMetadata.getStatus());
        }

        eventPublisher.publishEvent(
                new ImportCreatedEvent(importMetadata.getId(), content)
        );

        return new ImportResponse(importMetadata.getId(), importMetadata.getFileName(), importMetadata.getStatus());
    }

    @EventListener
    public void handleImportStatusUpdateEvent(ImportStatusUpdateEvent event) {
        log.info("Import {} metadata update. Status: {}. Total rows: {}, Errors: {}", event.id(), event.status(), event.totalRows(), event.errors().size());
        var importMetadata = importMetadataRepository.findById(event.id()).orElseThrow(() -> ImportMetadataException.notFound(event.id()));
        importMetadata.setStatus(event.status());
        importMetadata.setTotalRows(event.totalRows());
        importMetadata.setSuccessfulRows(event.successfulRows());
        importMetadata.setErrors(event.errors());

        importMetadataRepository.save(importMetadata);
    }

    @Transactional(readOnly = true)
    public ImportDetailsDto getImportDetails(String importId) {
        var importMetadata = importMetadataRepository.findById(importId).orElseThrow(() -> ImportMetadataException.notFound(importId));
        return new ImportDetailsDto(
                importMetadata.getId(),
                importMetadata.getFileName(),
                importMetadata.getStatus(),
                importMetadata.getTotalRows(),
                importMetadata.getSuccessfulRows(),
                importMetadata.getErrors());
    }
}
