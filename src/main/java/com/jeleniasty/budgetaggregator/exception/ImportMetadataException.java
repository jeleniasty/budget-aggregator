package com.jeleniasty.budgetaggregator.exception;

public class ImportMetadataException extends RuntimeException {
    private ImportMetadataException(String message) {
        super(message);
    }

    public static ImportMetadataException notFound(String importId) {
        return new ImportMetadataException("Import [id: " + importId + "] not found");
    }
}
