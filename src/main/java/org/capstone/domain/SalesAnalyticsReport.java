package org.capstone.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesAnalyticsReport {
    private Long id;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String aggregatedMetricsJson;
    private String aiInsightsText;
    private LocalDateTime createdAt;

    private Product product;
}
