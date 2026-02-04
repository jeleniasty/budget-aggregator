package com.jeleniasty.budgetaggregator.service.aggregator.mapper;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationRaw;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationSummary;
import com.jeleniasty.budgetaggregator.service.shared.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AggregationSummaryMapper {

    private final EncryptionService encryptionService;

    public AggregationSummary map(AggregationRaw rawResult, AggregationParameters params) {
        return new AggregationSummary(
                params.category() != null ? rawResult.category() : null,
                params.iban() != null ? encryptionService.decrypt(rawResult.iban()) : null,
                params.month() != null ? rawResult.month() : null,
                rawResult.currency(),
                rawResult.inflow(),
                rawResult.outflow(),
                rawResult.balance(),
                rawResult.transactionCount());
    }
}
