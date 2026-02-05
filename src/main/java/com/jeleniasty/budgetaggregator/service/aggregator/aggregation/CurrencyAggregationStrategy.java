package com.jeleniasty.budgetaggregator.service.aggregator.aggregation;

import com.jeleniasty.budgetaggregator.model.TransactionType;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import static com.jeleniasty.budgetaggregator.persistence.transaction.Transaction.Fields.AMOUNT;
import static com.jeleniasty.budgetaggregator.persistence.transaction.Transaction.Fields.CATEGORY;
import static com.jeleniasty.budgetaggregator.persistence.transaction.Transaction.Fields.CURRENCY;
import static com.jeleniasty.budgetaggregator.persistence.transaction.Transaction.Fields.IBAN;
import static com.jeleniasty.budgetaggregator.persistence.transaction.Transaction.Fields.TRANSACTION_DATE;
import static com.jeleniasty.budgetaggregator.persistence.transaction.Transaction.Fields.TRANSACTION_TYPE;

@Service
public class CurrencyAggregationStrategy implements AggregationStrategy{

    @Override
    public GroupOperation buildGroup(AggregationParameters params) {
        var creditExpression = ConditionalOperators.when(
                        Criteria.where(TRANSACTION_TYPE)
                                .is(TransactionType.CREDIT))
                .thenValueOf(AMOUNT).otherwise(0);

        var debitExpression = ConditionalOperators.when(
                        Criteria.where(TRANSACTION_TYPE)
                                .is(TransactionType.DEBIT))
                .thenValueOf(AMOUNT).otherwise(0);

        GroupOperation group = Aggregation.group(CURRENCY)
                .sum(creditExpression).as("inflow")
                .sum(debitExpression).as("outflow")
                .count().as("transactionCount")
                .first(CATEGORY).as(CATEGORY)
                .first(TRANSACTION_DATE).as("monthDate")
                .first(IBAN).as(IBAN);

        if (params.iban() != null) {
            group = group.first(IBAN).as(IBAN);
        }

        return group;    }

    @Override
    public ProjectionOperation buildProjection(AggregationParameters params) {
        return Aggregation.project("inflow", "outflow", "transactionCount")
                .and("_id").as(CURRENCY)
                .and(IBAN).as(IBAN)
                .and(DateOperators.dateOf("monthDate").toString("%Y-%m")).as("month")
                .andExpression("inflow - outflow").as("balance");
    }

    @Override
    public SortOperation buildSort(AggregationParameters params) {
        return Aggregation.sort(Sort.Direction.DESC, CURRENCY);
    }
}
