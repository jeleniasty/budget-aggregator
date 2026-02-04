package com.jeleniasty.budgetaggregator.service.aggregator.filter;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
public class MonthFilter implements FilterProvider {
    @Override
    public Optional<Criteria> build(AggregationParameters params) {
        if (params.month() == null) {
            return Optional.empty();
        }

        YearMonth ym = YearMonth.parse(params.month());

        Instant start = ym.atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant end = ym.atEndOfMonth()
                .atTime(LocalTime.MAX)
                .atOffset(ZoneOffset.UTC)
                .toInstant();

        return Optional.of(Criteria.where("transactionDate").gte(start).lte(end));
    }
}
