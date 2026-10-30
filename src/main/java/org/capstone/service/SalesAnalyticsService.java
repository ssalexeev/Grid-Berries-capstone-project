package org.capstone.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.domain.SalesAnalyticsReport;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.UserMetricEvent;
import org.capstone.domain.enums.MetricEventType;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.repository.jdbc.OrderRepository;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.SalesAnalyticsReportRepository;
import org.capstone.repository.jdbc.SellerProfileRepository;
import org.capstone.repository.jdbc.UserMetricEventRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesAnalyticsService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserMetricEventRepository eventRepository;
    private final SalesAnalyticsReportRepository reportRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ObjectMapper objectMapper;

    public SalesMetrics calculateMetrics(
            Long sellerProfileId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd) {
        validateRequest(sellerProfileId, periodStart, periodEnd);
        Set<Long> sellerProductIds = productRepository.findBySellerId(sellerProfileId).stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        List<Order> sellerOrders = orderRepository.findAll().stream()
                .filter(order -> withinRange(order.getCreatedAt(), periodStart, periodEnd))
                .filter(order -> hasSellerProduct(order, sellerProductIds))
                .toList();

        BigDecimal revenue = BigDecimal.ZERO;
        long unitsSold = 0;
        for (Order order : sellerOrders) {
            if (order.getStatus() == OrderStatus.COMPLETED) {
                for (OrderItem item : order.getItems()) {
                    if (item.getProduct() != null && sellerProductIds.contains(item.getProduct().getId())) {
                        revenue = revenue.add(item.getSubtotal());
                        unitsSold += item.getQuantity() == null ? 0 : item.getQuantity();
                    }
                }
            }
        }

        long views = 0;
        long purchases = 0;
        for (UserMetricEvent event : eventRepository.findAll()) {
            if (event.getProduct() == null
                    || !sellerProductIds.contains(event.getProduct().getId())
                    || !withinRange(event.getTimestamp(), periodStart, periodEnd)) {
                continue;
            }
            if (event.getEventType() == MetricEventType.VIEW) {
                views++;
            } else if (event.getEventType() == MetricEventType.PURCHASE) {
                purchases++;
            }
        }

        long cancelledOrders = sellerOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
                .count();
        long completedOrders = sellerOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .count();
        BigDecimal conversionRate = percentage(purchases, views);
        BigDecimal cancellationRate = percentage(cancelledOrders, sellerOrders.size());
        return new SalesMetrics(
                revenue,
                unitsSold,
                views,
                purchases,
                conversionRate,
                sellerOrders.size(),
                completedOrders,
                cancelledOrders,
                cancellationRate,
                periodStart,
                periodEnd);
    }

    public SalesAnalyticsReport createReport(
            Long sellerProfileId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd) {
        SalesMetrics metrics = calculateMetrics(sellerProfileId, periodStart, periodEnd);
        String metricsJson;
        try {
            metricsJson = objectMapper.writeValueAsString(metrics);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize sales metrics.", e);
        }
        return reportRepository.save(SalesAnalyticsReport.builder()
                .product(null)
                .periodStart(metrics.periodStart())
                .periodEnd(metrics.periodEnd())
                .aggregatedMetricsJson(metricsJson)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private boolean hasSellerProduct(Order order, Set<Long> sellerProductIds) {
        return order.getItems().stream()
                .map(OrderItem::getProduct)
                .anyMatch(product -> product != null && sellerProductIds.contains(product.getId()));
    }

    private boolean withinRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && !value.isAfter(end);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private void validateRequest(Long sellerProfileId, LocalDateTime start, LocalDateTime end) {
        getSeller(sellerProfileId);
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("A valid analytics date range is required.");
        }
    }

    private SellerProfile getSeller(Long sellerProfileId) {
        if (sellerProfileId == null) {
            throw new IllegalArgumentException("Seller profile ID cannot be null.");
        }
        return sellerProfileRepository.findById(sellerProfileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seller profile not found with ID: " + sellerProfileId));
    }

    public record SalesMetrics(
            BigDecimal revenue,
            long unitsSold,
            long views,
            long purchases,
            BigDecimal conversionRatePercent,
            long orderCount,
            long completedOrders,
            long cancelledOrders,
            BigDecimal cancellationRatePercent,
            LocalDateTime periodStart,
            LocalDateTime periodEnd) {
    }
}
