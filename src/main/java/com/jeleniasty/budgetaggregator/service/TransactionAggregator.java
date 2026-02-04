package com.jeleniasty.budgetaggregator.service;

import com.jeleniasty.budgetaggregator.model.TransactionType;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationRaw;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionAggregator {

    private final MongoTemplate mongoTemplate;
    private final EncryptionService encryptionService;

    public List<AggregationSummary> aggregate(AggregationParameters params, Pageable pageable) {

        List<Criteria> filters = new ArrayList<>();

        Optional.ofNullable(params.iban())
                .map(encryptionService::generateBlindIndex)
                .ifPresent(hash -> filters.add(Criteria.where("ibanHash").is(hash)));

        Optional.ofNullable(params.category())
                .ifPresent(c -> filters.add(Criteria.where("category").is(c)));

        if (params.month() != null) {
            YearMonth ym = YearMonth.parse(params.month());

            Instant start = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = ym.atEndOfMonth().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toInstant();

            filters.add(Criteria.where("transactionDate").gte(start).lte(end));
        }

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

        ProjectionOperation finalProject = Aggregation.project("inflow", "outflow", "transactionCount", "category")
                .and("_id").as("currency")
                .and("encryptedIban").as("iban")
                .and(
                        DateOperators.dateOf("anyDate").toString("%Y-%m")
                ).as("month")
                .andExpression("inflow - outflow").as("balance")
                .andExclude("_id");

        SortOperation sortStage = pageable.getSort().isSorted()
                ? Aggregation.sort(pageable.getSort())
                : Aggregation.sort(Sort.Direction.DESC, "currency");

        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                groupStage,
                finalProject,
                sortStage,
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize())
        ).withOptions(
                Aggregation.newAggregationOptions()
                        .allowDiskUse(true)
                        .build()
        );

        var results = mongoTemplate.aggregate(aggregation, "transactions", AggregationRaw.class)
                .getMappedResults();

        return results.stream()
                .map(result -> new AggregationSummary(
                        result.category(),
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
