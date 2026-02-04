package com.jeleniasty.budgetaggregator.api;

import com.jeleniasty.budgetaggregator.model.imports.ImportDetailsDto;
import com.jeleniasty.budgetaggregator.model.imports.ImportResponse;
import com.jeleniasty.budgetaggregator.service.imports.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "Import transactions from a file",
            description = "Uploads a CSV/Excel file and starts the transaction import process asynchronously."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Import accepted",
                    content = @Content(schema = @Schema(implementation = ImportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or request"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResponse> importTransactions(@RequestParam("file") MultipartFile file) {
        var importResponse = importService.importFile(file);
        return ResponseEntity.accepted()
                .header(HttpHeaders.LOCATION, "/transactions/imports/" + importResponse.id())
                .body(importResponse);
    }


    @Operation(
            summary = "Get import status",
            description = "Returns the details and status of a previously started import by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Import details returned",
                    content = @Content(schema = @Schema(implementation = ImportDetailsDto.class))),
            @ApiResponse(responseCode = "400", description = "Import ID not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping(value = "/{importId}")
    public ResponseEntity<ImportDetailsDto> getImportStatus(@PathVariable String importId) {
        return ResponseEntity.ok(importService.getImportDetails(importId));
    }
}
