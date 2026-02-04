package com.jeleniasty.budgetaggregator.model.imports;

import java.util.List;

public record ImportDetailsDto(String id, String fileName, ImportStatus status, int totalRows, int successfulRows, List<String> errors) {
}
