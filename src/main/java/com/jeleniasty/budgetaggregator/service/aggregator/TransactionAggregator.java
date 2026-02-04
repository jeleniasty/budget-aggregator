package com.jeleniasty.budgetaggregator.service.aggregator;

import com.jeleniasty.budgetaggregator.model.TransactionType;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationRaw;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationSummary;
import com.jeleniasty.budgetaggregator.service.aggregator.filter.FilterProvider;
import com.jeleniasty.budgetaggregator.service.shared.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionAggregator {

    private final MongoTemplate mongoTemplate;
    private final EncryptionService encryptionService;
    private final List<FilterProvider> filterProviders;

    public List<AggregationSummary> aggregate(AggregationParameters params) {

        List<Criteria> filters = filterProviders.stream()
                .map(f -> f.build(params))
                .flatMap(Optional::stream)
                .toList();

        MatchOperation matchStage = Aggregation.match(
                filters.isEmpty()
                        ? new Criteria()
                        : new Criteria().andOperator(filters.toArray(new Criteria[0]))
        );

        var creditExpression = ConditionalOperators.when(
                        Criteria.where("transactionType").is(TransactionType.CREDIT))
                .thenValueOf("amount").otherwise(0);

        var debitExpression = ConditionalOperators.when(
                        Criteria.where("transactionType").is(TransactionType.DEBIT))
                .thenValueOf("amount").otherwise(0);

        GroupOperation groupStage = Aggregation.group("currency")
                .sum(creditExpression).as("inflow")
                .sum(debitExpression).as("outflow")
                .count().as("transactionCount")
                .first("category").as("category")
                .first("transactionDate").as("monthDate");

        if (params.iban() != null) {
            groupStage = groupStage.first("iban").as("encryptedIban");
        }

        ProjectionOperation finalProject = Aggregation.project("inflow", "outflow", "transactionCount")
                .and("_id").as("currency")
                .and("encryptedIban").as("iban")
                .and(
                        DateOperators.dateOf("monthDate").toString("%Y-%m")
                ).as("month")
                .andExpression("inflow - outflow").as("balance");

        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "currency");

        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                groupStage,
                finalProject,
                sortStage
        ).withOptions(
                Aggregation.newAggregationOptions()
                        .allowDiskUse(true)
                        .build()
        );

        var results = mongoTemplate.aggregate(aggregation, "transactions", AggregationRaw.class)
                .getMappedResults();

        return results.stream()
                .map(result -> new AggregationSummary(
                        params.category() != null ? result.category() : null,
                        params.iban() != null ? encryptionService.decrypt(result.iban()) : null,
                        params.month() != null ? result.month() : null,
                        result.currency(),
                        result.inflow(),
                        result.outflow(),
                        result.balance(),
                        result.transactionCount()
                ))
                .toList();
    }
}
