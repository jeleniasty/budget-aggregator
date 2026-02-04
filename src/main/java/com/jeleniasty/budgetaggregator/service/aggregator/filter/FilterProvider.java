package com.jeleniasty.budgetaggregator.service.aggregator.filter;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface FilterProvider {
    Optional<Criteria> build(AggregationParameters params);
}
