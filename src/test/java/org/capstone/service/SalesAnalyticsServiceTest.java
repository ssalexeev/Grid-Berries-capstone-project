package org.capstone.service;

import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.UserMetricEvent;
import org.capstone.domain.enums.MetricEventType;
import org.capstone.domain.enums.OrderStatus;
import org.capstone.repository.jdbc.OrderRepository;
import org.capstone.repository.jdbc.ProductRepository;
import org.capstone.repository.jdbc.SalesAnalyticsReportRepository;
import org.capstone.repository.jdbc.SellerProfileRepository;
import org.capstone.repository.jdbc.UserMetricEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserMetricEventRepository eventRepository;
    @Mock
    private SalesAnalyticsReportRepository reportRepository;
    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @InjectMocks
    private SalesAnalyticsService salesAnalyticsService;

    @Test
    void calculateMetricsAggregatesSalesViewsAndCancellationsForPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        SellerProfile seller = SellerProfile.builder().id(3L).build();
        Product product = Product.builder().id(4L).sellerProfile(seller).build();
        Order completed = order(1L, product, OrderStatus.COMPLETED, start.plusDays(2), 2);
        Order cancelled = order(2L, product, OrderStatus.CANCELLED, start.plusDays(3), 1);
        when(sellerProfileRepository.findById(3L)).thenReturn(Optional.of(seller));
        when(productRepository.findBySellerId(3L)).thenReturn(List.of(product));
        when(orderRepository.findAll()).thenReturn(List.of(completed, cancelled));
        when(eventRepository.findAll()).thenReturn(List.of(
                event(product, MetricEventType.VIEW, start.plusDays(1)),
                event(product, MetricEventType.VIEW, start.plusDays(2)),
                event(product, MetricEventType.VIEW, start.plusDays(3)),
                event(product, MetricEventType.VIEW, start.plusDays(4)),
                event(product, MetricEventType.PURCHASE, start.plusDays(2)),
                event(product, MetricEventType.PURCHASE, start.plusDays(3))));

        SalesAnalyticsService.SalesMetrics metrics =
                salesAnalyticsService.calculateMetrics(3L, start, end);

        assertEquals(new BigDecimal("15.00"), metrics.revenue());
        assertEquals(2, metrics.unitsSold());
        assertEquals(4, metrics.views());
        assertEquals(2, metrics.purchases());
        assertEquals(new BigDecimal("50.00"), metrics.conversionRatePercent());
        assertEquals(2, metrics.orderCount());
        assertEquals(1, metrics.cancelledOrders());
        assertEquals(new BigDecimal("50.00"), metrics.cancellationRatePercent());
    }

    private Order order(Long id, Product product, OrderStatus status, LocalDateTime createdAt, int quantity) {
        return Order.builder()
                .id(id)
                .status(status)
                .createdAt(createdAt)
                .items(List.of(OrderItem.builder()
                        .product(product)
                        .quantity(quantity)
                        .unitPrice(new BigDecimal("7.50"))
                        .build()))
                .build();
    }

    private UserMetricEvent event(Product product, MetricEventType type, LocalDateTime timestamp) {
        return UserMetricEvent.builder().product(product).eventType(type).timestamp(timestamp).build();
    }
}
