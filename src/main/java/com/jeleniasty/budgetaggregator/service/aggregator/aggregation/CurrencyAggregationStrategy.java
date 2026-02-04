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
import org.springframework.stereotype.Service;

@Service
public class CurrencyAggregationStrategy implements AggregationStrategy{

    @Override
    public GroupOperation buildGroup(AggregationParameters params) {
        var creditExpression = ConditionalOperators.when(
                        org.springframework.data.mongodb.core.query.Criteria.where("transactionType")
                                .is(TransactionType.CREDIT))
                .thenValueOf("amount").otherwise(0);

        var debitExpression = ConditionalOperators.when(
                        org.springframework.data.mongodb.core.query.Criteria.where("transactionType")
                                .is(TransactionType.DEBIT))
                .thenValueOf("amount").otherwise(0);

        GroupOperation group = Aggregation.group("currency")
                .sum(creditExpression).as("inflow")
                .sum(debitExpression).as("outflow")
                .count().as("transactionCount")
                .first("category").as("category")
                .first("transactionDate").as("monthDate");

        if (params.iban() != null) {
            group = group.first("iban").as("encryptedIban");
        }

        return group;    }

    @Override
    public ProjectionOperation buildProjection(AggregationParameters params) {
        return Aggregation.project("inflow", "outflow", "transactionCount")
                .and("_id").as("currency")
                .and("encryptedIban").as("iban")
                .and(DateOperators.dateOf("monthDate").toString("%Y-%m")).as("month")
                .andExpression("inflow - outflow").as("balance");    }

    @Override
    public SortOperation buildSort(AggregationParameters params) {
        return Aggregation.sort(Sort.Direction.DESC, "currency");
    }
}
