package com.jeleniasty.budgetaggregator.service;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationSummary;
import com.jeleniasty.budgetaggregator.model.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
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

        //TODO transactionDate dont work
        List<Criteria> filters = new ArrayList<>();

        Optional.ofNullable(params.iban())
                .map(encryptionService::generateBlindIndex)
                .ifPresent(hash -> filters.add(Criteria.where("ibanHash").is(hash)));
        Optional.ofNullable(params.category()).ifPresent(c -> filters.add(Criteria.where("category").is(c)));

        if (params.month() != null) {
            YearMonth yearMonth = YearMonth.parse(params.month());
            Instant start = yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
            filters.add(Criteria.where("transactionDate").gte(start).lte(end));
        }

        MatchOperation matchStage = Aggregation.match(
                filters.isEmpty() ? new Criteria() : new Criteria().andOperator(filters.toArray(new Criteria[0]))
        );

        ProjectionOperation projectStage = Aggregation.project("category", "iban", "amount", "transactionType")
                .and("transactionDate").dateAsFormattedString("%Y-%m").as("month");


        GroupOperation groupStage = Aggregation.group("category", "ibanHash", "month")
                .sum(ConditionalOperators.when(Criteria.where("transactionType").is(TransactionType.CREDIT))
                        .thenValueOf("amount").otherwise(0)).as("inflow")
                .sum(ConditionalOperators.when(Criteria.where("transactionType").is(TransactionType.DEBIT))
                        .thenValueOf("amount").otherwise(0)).as("outflow")
                .count().as("transactionCount")
                .first("iban").as("encryptedIban");


        ProjectionOperation finalProject = Aggregation.project("inflow", "outflow", "transactionCount")
                .and("_id.category").as("category")
                .and("encryptedIban").as("iban")
                .and("_id.month").as("month")
                .andExpression("inflow - outflow").as("balance")
                .andExclude("_id");

        SortOperation sortStage = Aggregation.sort(pageable.getSort().isSorted() ? pageable.getSort() :
                Sort.by(Sort.Direction.DESC, "month"));

        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                projectStage,
                groupStage,
                finalProject,
                sortStage,
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize())
        ).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());

        var results = mongoTemplate.aggregate(aggregation, "transactions", AggregationSummary.class)
                .getMappedResults();

        return results.stream()
                .map(r -> new AggregationSummary(
                        r.category(),
                        encryptionService.decrypt(r.iban()),
                        r.month(),
                        r.inflow(),
                        r.outflow(),
                        r.balance(),
                        r.transactionCount()
                ))
                .toList();
    }
}
