package com.jeleniasty.budgetaggregator.service.aggregator.aggregation;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;

public interface AggregationStrategy {
    GroupOperation buildGroup(AggregationParameters params);
    ProjectionOperation buildProjection(AggregationParameters params);
    SortOperation buildSort(AggregationParameters params);
}
