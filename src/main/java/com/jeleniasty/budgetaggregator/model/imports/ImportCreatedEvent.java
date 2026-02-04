package com.jeleniasty.budgetaggregator.model.imports;

public record ImportCreatedEvent(String importId, byte[] content) {
}
