package com.jeleniasty.budgetaggregator.service.aggregator.filter;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.jeleniasty.budgetaggregator.persistence.transaction.Transaction.Fields.CATEGORY;

@Component
public class CategoryFilter implements FilterProvider{
    public Optional<Criteria> build(AggregationParameters params) {
        return Optional.ofNullable(params.category())
                .map(category -> Criteria.where(CATEGORY).is(category));
    }
}
