package com.jeleniasty.budgetaggregator.api;

import com.jeleniasty.budgetaggregator.model.aggregation.AggregationParameters;
import com.jeleniasty.budgetaggregator.model.aggregation.AggregationSummary;
import com.jeleniasty.budgetaggregator.service.aggregator.TransactionAggregator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "Get transaction aggregation stats",
            description = "Returns aggregated transaction stats by currency. Possible filters: category, iban and/or month")
    @ApiResponse(responseCode = "200", description = "Aggregated stats returned successfully")
    @PostMapping
    public ResponseEntity<List<AggregationSummary>> getStats(
            @Valid @RequestBody AggregationParameters params) {

        var stats = aggregator.aggregate(params);
        return ResponseEntity.ok(stats);
    }
}
