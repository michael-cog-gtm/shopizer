package com.salesmanager.test.shop.integration.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import com.salesmanager.core.business.repositories.order.DailyOrderAggregate;
import com.salesmanager.core.business.services.order.DailyOrderSummaryService;
import com.salesmanager.core.model.order.orderstatus.OrderStatus;
import com.salesmanager.shop.model.order.ReadableDailyOrderSummary;
import com.salesmanager.shop.store.api.v1.order.DailyOrderSummaryMapper;

class DailyOrderSummaryMapperTest {
    private final LocalDate start = LocalDate.of(2024, 2, 28);

    @Test
    void fillsDaysAndKeepsCurrenciesAndStatusesSeparate() {
        ReadableDailyOrderSummary result = DailyOrderSummaryMapper.map("A", start, start.plusDays(2), Arrays.asList(
                row(start, OrderStatus.ORDERED, "USD", 2, "12.35"),
                row(start, OrderStatus.DELIVERED, "USD", 1, "5.10"),
                row(start, OrderStatus.ORDERED, "JPY", 1, "100"),
                row(start.plusDays(2), OrderStatus.ORDERED, "USD", 1, "2.00")));
        assertEquals(3, result.getDays().size());
        assertEquals("2024-02-29", result.getDays().get(1).getDate());
        assertEquals(0, result.getDays().get(1).getOrderCount());
        assertTrue(result.getDays().get(1).getBreakdown().isEmpty());
        assertEquals(4, result.getDays().get(0).getOrderCount());
        assertEquals(3, result.getDays().get(0).getBreakdown().size());
        assertEquals(2, result.getTotals().size());
        assertEquals(new BigDecimal("19.45"), result.getTotals().get(0).getTotal());
        assertEquals(4, result.getTotals().get(0).getOrderCount());
        assertEquals(new BigDecimal("100"), result.getTotals().get(1).getTotal());
        assertEquals(new BigDecimal("14.35"), result.getBreakdown().get(0).getTotal());
        assertEquals(3, result.getBreakdown().get(0).getOrderCount());
    }

    @Test
    void emptyRangeAndSingleDayAreVisible() {
        ReadableDailyOrderSummary result = DailyOrderSummaryMapper.map("A", start, start, Collections.emptyList());
        assertEquals(1, result.getDays().size());
        assertEquals(0, result.getDays().get(0).getOrderCount());
        assertTrue(result.getTotals().isEmpty());
        assertTrue(result.getBreakdown().isEmpty());
    }

    @Test
    void validatesInclusiveRangeLimit() {
        assertDoesNotThrow(() -> DailyOrderSummaryService.validateRange(start, start.plusDays(365)));
        assertThrows(IllegalArgumentException.class, () -> DailyOrderSummaryService.validateRange(start, start.plusDays(366)));
        assertThrows(IllegalArgumentException.class, () -> DailyOrderSummaryService.validateRange(start, start.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> DailyOrderSummaryService.validateRange(null, start));
        assertThrows(IllegalArgumentException.class, () -> DailyOrderSummaryService.validateRange(start, null));
    }

    private DailyOrderAggregate row(LocalDate date, OrderStatus status, String currency, long count, String total) {
        DailyOrderAggregate row = mock(DailyOrderAggregate.class);
        when(row.getDay()).thenReturn(Date.valueOf(date));
        when(row.getStatus()).thenReturn(status);
        when(row.getCurrency()).thenReturn(currency);
        when(row.getOrderCount()).thenReturn(count);
        when(row.getTotal()).thenReturn(new BigDecimal(total));
        return row;
    }
}
