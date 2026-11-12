package com.salesmanager.shop.store.api.v1.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.salesmanager.core.business.repositories.order.DailyOrderAggregate;
import com.salesmanager.core.business.services.order.DailyOrderSummaryService;
import com.salesmanager.shop.model.order.ReadableDailyOrderSummary;
import com.salesmanager.shop.model.order.ReadableDailyOrderSummary.ReadableCurrencyTotal;
import com.salesmanager.shop.model.order.ReadableDailyOrderSummary.ReadableOrderDay;
import com.salesmanager.shop.model.order.ReadableDailyOrderSummary.ReadableStatusTotal;

public final class DailyOrderSummaryMapper {
    private DailyOrderSummaryMapper() { }

    public static ReadableDailyOrderSummary map(String store, LocalDate start, LocalDate end,
            List<DailyOrderAggregate> aggregates) {
        DailyOrderSummaryService.validateRange(start, end);
        ReadableDailyOrderSummary result = new ReadableDailyOrderSummary();
        result.setStore(store);
        result.setStartDate(start.toString());
        result.setEndDate(end.toString());
        Map<LocalDate, ReadableOrderDay> days = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            ReadableOrderDay day = new ReadableOrderDay();
            day.setDate(date.toString());
            days.put(date, day);
        }
        for (DailyOrderAggregate row : aggregates) {
            LocalDate date = new Date(row.getDay().getTime()).toLocalDate();
            ReadableOrderDay day = days.get(date);
            String status = row.getStatus() == null ? null : row.getStatus().name();
            BigDecimal total = row.getTotal() == null ? BigDecimal.ZERO : row.getTotal();
            day.setOrderCount(day.getOrderCount() + row.getOrderCount());
            addStatus(day.getBreakdown(), status, row.getCurrency(), row.getOrderCount(), total);
            addCurrency(day.getTotals(), row.getCurrency(), row.getOrderCount(), total);
            addStatus(result.getBreakdown(), status, row.getCurrency(), row.getOrderCount(), total);
            addCurrency(result.getTotals(), row.getCurrency(), row.getOrderCount(), total);
        }
        result.getDays().addAll(days.values());
        result.getDays().forEach(day -> {
            day.getBreakdown().forEach(DailyOrderSummaryMapper::round);
            day.getTotals().forEach(DailyOrderSummaryMapper::round);
        });
        result.getBreakdown().forEach(DailyOrderSummaryMapper::round);
        result.getTotals().forEach(DailyOrderSummaryMapper::round);
        return result;
    }

    private static void addStatus(List<ReadableStatusTotal> entries, String status, String currency,
            long count, BigDecimal total) {
        ReadableStatusTotal entry = entries.stream().filter(e -> Objects.equals(e.getStatus(), status)
                && Objects.equals(e.getCurrency(), currency)).findFirst().orElse(null);
        if (entry == null) {
            entry = new ReadableStatusTotal();
            entry.setStatus(status);
            entry.setCurrency(currency);
            entry.setTotal(BigDecimal.ZERO);
            entries.add(entry);
        }
        add(entry, count, total);
    }

    private static void addCurrency(List<ReadableCurrencyTotal> entries, String currency, long count,
            BigDecimal total) {
        ReadableCurrencyTotal entry = entries.stream().filter(e -> Objects.equals(e.getCurrency(), currency))
                .findFirst().orElse(null);
        if (entry == null) {
            entry = new ReadableCurrencyTotal();
            entry.setCurrency(currency);
            entry.setTotal(BigDecimal.ZERO);
            entries.add(entry);
        }
        add(entry, count, total);
    }

    private static void add(ReadableCurrencyTotal entry, long count, BigDecimal total) {
        entry.setOrderCount(entry.getOrderCount() + count);
        entry.setTotal(entry.getTotal().add(total));
    }

    private static void round(ReadableCurrencyTotal entry) {
        int scale = 2;
        try {
            int digits = java.util.Currency.getInstance(entry.getCurrency()).getDefaultFractionDigits();
            if (digits >= 0) { scale = digits; }
        } catch (IllegalArgumentException | NullPointerException ignored) { }
        entry.setTotal(entry.getTotal().setScale(scale, RoundingMode.HALF_UP));
    }
}
