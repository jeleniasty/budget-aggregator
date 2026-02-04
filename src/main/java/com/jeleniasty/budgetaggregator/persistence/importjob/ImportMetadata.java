package com.jeleniasty.budgetaggregator.persistence.importjob;

import com.jeleniasty.budgetaggregator.model.imports.ImportStatus;
import com.jeleniasty.budgetaggregator.persistence.AuditableDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "imports")
@Getter
@Setter
public class ImportMetadata extends AuditableDocument {
    @Id
    private String id;
    private String fileName;
    private ImportStatus status;
    private int totalRows;
    private int successfulRows;
    List<String> errors;

    public ImportMetadata(String fileName, ImportStatus status) {
        this.fileName = fileName;
        this.status = status;
    }
}
