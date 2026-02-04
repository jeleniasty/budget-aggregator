package com.jeleniasty.budgetaggregator.model.events;

import com.jeleniasty.budgetaggregator.model.ImportStatus;

import java.util.List;

public record ImportStatusUpdateEvent(String id, ImportStatus status, int totalRows, int successfulRows, List<String> errors) {
}
