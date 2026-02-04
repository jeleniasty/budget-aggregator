package com.jeleniasty.budgetaggregator.service;

import com.jeleniasty.budgetaggregator.model.TransactionDto;
import com.jeleniasty.budgetaggregator.persistence.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionDataService {
    private final MongoTemplate mongoTemplate;
    private final EncryptionService encryptionService;

    public void saveBatch(List<TransactionDto> batch, String importId) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Transaction.class);

        List<Transaction> documents = batch.stream()
                .map(dto -> mapToDocument(dto, importId))
                .toList();

        bulkOps.insert(documents);
        bulkOps.execute();
    }

    private Transaction mapToDocument(TransactionDto dto, String importId) {
        var ibanCipher = encryptionService.encrypt(dto.iban());
        var ibanHash = encryptionService.generateBlindIndex(dto.iban());
        return new Transaction(dto.bankId(), dto.referenceId(), ibanCipher, ibanHash, dto.transactionDate(), dto.currency(), dto.category(), dto.transactionType(), dto.amount(), importId);
    }
}
