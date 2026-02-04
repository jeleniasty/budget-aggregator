package com.jeleniasty.budgetaggregator.service.aggregator.filter;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import com.jeleniasty.budgetaggregator.service.shared.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IbanFilter implements FilterProvider {

    private final EncryptionService encryptionService;

    public Optional<Criteria> build(AggregationParameters params) {
        return Optional.ofNullable(params.iban())
                .map(encryptionService::generateBlindIndex)
                .map(hash -> Criteria.where("ibanHash").is(hash));
    }
}
