package com.jeleniasty.budgetaggregator.api;

import com.jeleniasty.budgetaggregator.model.ImportDetailsDto;
import com.jeleniasty.budgetaggregator.model.ImportResponse;
import com.jeleniasty.budgetaggregator.service.imports.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/transactions/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResponse> importTransactions(@RequestParam("file") MultipartFile file) {
        var importResponse = importService.importFile(file);
        return ResponseEntity.accepted()
                .header(HttpHeaders.LOCATION, "/transactions/imports/" + importResponse.id())
                .body(importResponse);
    }

    @GetMapping(value = "/{importId}")
    public ResponseEntity<ImportDetailsDto> getImportStatus(@PathVariable String importId) {
        return ResponseEntity.ok(importService.getImportDetails(importId));
    }
}
