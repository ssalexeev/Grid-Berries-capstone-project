package org.capstone.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.capstone.domain.SalesAnalyticsReport;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class SalesAnalyticsReportRepository implements JdbcRepository<SalesAnalyticsReport, Long> {
    private final JdbcUtil jdbcUtil;
    private final ProductRepository productRepository;

    @Override
    public Optional<SalesAnalyticsReport> findById(Long id) {
        return jdbcUtil.findOne("SELECT * FROM sales_analytics_report WHERE id = ?", mapper(), id);
    }

    @Override
    public List<SalesAnalyticsReport> findAll() {
        return jdbcUtil.findMany("SELECT * FROM sales_analytics_report", mapper());
    }

    public List<SalesAnalyticsReport> findAllByProductId(Long productId) {
        return jdbcUtil.findMany(
                "SELECT * FROM sales_analytics_report WHERE product_id = ?",
                mapper(),
                productId
        );
    }

    @Override
    public SalesAnalyticsReport save(SalesAnalyticsReport report) {
        if (report == null) {
            throw new IllegalArgumentException("Sales analytics report cannot be null");
        }
        Long productId = report.getProduct() == null ? null : report.getProduct().getId();
        LocalDate periodStart = report.getPeriodStart() == null ? null : report.getPeriodStart().toLocalDate();
        LocalDate periodEnd = report.getPeriodEnd() == null ? null : report.getPeriodEnd().toLocalDate();
        if (report.getId() == null) {
            long id = jdbcUtil.insertAndReturnId("""
                    INSERT INTO sales_analytics_report
                        (product_id, period_start, period_end, aggregated_metrics, ai_insights_text, created_at)
                    VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?)
                    RETURNING id
                    """, productId, periodStart, periodEnd, report.getAggregatedMetricsJson(),
                    report.getAiInsightsText(), report.getCreatedAt());
            report.setId(id);
        } else {
            jdbcUtil.execute("""
                    UPDATE sales_analytics_report
                    SET product_id = ?, period_start = ?, period_end = ?, aggregated_metrics = CAST(? AS jsonb),
                        ai_insights_text = ?, created_at = ?
                    WHERE id = ?
                    """, productId, periodStart, periodEnd, report.getAggregatedMetricsJson(),
                    report.getAiInsightsText(), report.getCreatedAt(), report.getId());
        }
        return findById(report.getId()).orElse(null);
    }

    @Override
    public void delete(SalesAnalyticsReport report) {
        if (report == null || report.getId() == null) {
            throw new IllegalArgumentException("Sales analytics report and ID cannot be null");
        }
        jdbcUtil.execute("DELETE FROM sales_analytics_report WHERE id = ?", report.getId());
    }

    @Override
    public Function<ResultSet, SalesAnalyticsReport> mapper() {
        return resultSet -> {
            try {
                Date periodStart = resultSet.getDate("period_start");
                Date periodEnd = resultSet.getDate("period_end");
                Timestamp createdAt = resultSet.getTimestamp("created_at");
                Long productId = nullableLong(resultSet, "product_id");
                return SalesAnalyticsReport.builder()
                        .id(resultSet.getLong("id"))
                        .periodStart(periodStart == null ? null : periodStart.toLocalDate().atStartOfDay())
                        .periodEnd(periodEnd == null ? null : periodEnd.toLocalDate().atStartOfDay())
                        .aggregatedMetricsJson(resultSet.getString("aggregated_metrics"))
                        .aiInsightsText(resultSet.getString("ai_insights_text"))
                        .createdAt(createdAt == null ? null : createdAt.toLocalDateTime())
                        .product(productId == null ? null : productRepository.findById(productId).orElse(null))
                        .build();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to map ResultSet to SalesAnalyticsReport", e);
            }
        };
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
