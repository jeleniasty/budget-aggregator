package com.jeleniasty.budgetaggregator.persistence.transaction;

import com.jeleniasty.budgetaggregator.model.TransactionType;
import com.jeleniasty.budgetaggregator.persistence.AuditableDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

import static com.jeleniasty.budgetaggregator.persistence.transaction.Transaction.TRANSACTIONS_DOCUMENT;

@Document(TRANSACTIONS_DOCUMENT)
@Getter
@Setter
@CompoundIndex(
        name = "bank_ref_unique",
        def = "{'bankId': 1, 'referenceId': 1}",
        unique = true)
@CompoundIndex(
        name = "idx_iban_category_date_currency",
        def = "{'ibanHash': 1, 'category': 1, 'transactionDate': -1, 'currency': 1}"
)
public class Transaction extends AuditableDocument {
    public static final String TRANSACTIONS_DOCUMENT = "transactions";

    @Id
    private String id;
    private String bankId;
    private String referenceId;
    private String iban;
    private String ibanHash;
    private Instant transactionDate;
    private String currency;
    private String category;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String importId;

    public Transaction(String bankId, String referenceId, String iban, String ibanHash, Instant transactionDate, String currency, String category, TransactionType transactionType, BigDecimal amount, String importId) {
        this.bankId = bankId;
        this.referenceId = referenceId;
        this.iban = iban;
        this.ibanHash = ibanHash;
        this.transactionDate = transactionDate;
        this.currency = currency;
        this.category = category;
        this.transactionType = transactionType;
        this.amount = amount;
        this.importId = importId;
    }

    public static final class Fields {
        public static final String ID = "id";
        public static final String BANK_ID = "bankId";
        public static final String REFERENCE_ID = "referenceId";
        public static final String IBAN = "iban";
        public static final String IBAN_HASH = "ibanHash";
        public static final String TRANSACTION_DATE = "transactionDate";
        public static final String CURRENCY = "currency";
        public static final String CATEGORY = "category";
        public static final String TRANSACTION_TYPE = "transactionType";
        public static final String AMOUNT = "amount";
        public static final String IMPORT_ID = "importId";
    }
}
