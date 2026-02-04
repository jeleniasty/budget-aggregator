package com.jeleniasty.budgetaggregator.api;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationSummary;
import com.jeleniasty.budgetaggregator.service.TransactionAggregator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions/stats")
@RequiredArgsConstructor
public class AggregationController {

    private final TransactionAggregator aggregator;

    @PostMapping
    public ResponseEntity<List<AggregationSummary>> getUniversalStats(
            @Valid @RequestBody AggregationParameters params,
            Pageable pageable) {

        var stats = aggregator.aggregate(params, pageable);
        return ResponseEntity.ok(stats);
    }
}
