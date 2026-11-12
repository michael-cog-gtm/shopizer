package com.salesmanager.shop.model.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Daily order summary; monetary totals are never combined across currencies")
public class ReadableDailyOrderSummary {
    private String store;
    private String startDate;
    private String endDate;
    private List<ReadableOrderDay> days = new ArrayList<>();
    private List<ReadableStatusTotal> breakdown = new ArrayList<>();
    private List<ReadableCurrencyTotal> totals = new ArrayList<>();

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }
    @ApiModelProperty(example = "2026-01-01", required = true)
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    @ApiModelProperty(example = "2026-01-31", required = true)
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public List<ReadableOrderDay> getDays() { return days; }
    public List<ReadableStatusTotal> getBreakdown() { return breakdown; }
    public List<ReadableCurrencyTotal> getTotals() { return totals; }

    @ApiModel(description = "One stored DATE_PURCHASED calendar day, including empty days")
    public static class ReadableOrderDay {
        private String date;
        private long orderCount;
        private List<ReadableStatusTotal> breakdown = new ArrayList<>();
        private List<ReadableCurrencyTotal> totals = new ArrayList<>();
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public List<ReadableStatusTotal> getBreakdown() { return breakdown; }
        public List<ReadableCurrencyTotal> getTotals() { return totals; }
    }

    @ApiModel(description = "Order count and sum of ORDER_TOTAL for a single currency")
    public static class ReadableCurrencyTotal {
        private String currency;
        private long orderCount;
        private BigDecimal total;
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        @ApiModelProperty(value = "Sum rounded to currency fraction digits, or 2 if unavailable")
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
    }

    @ApiModel(description = "Order count and total for a single status and currency")
    public static class ReadableStatusTotal extends ReadableCurrencyTotal {
        private String status;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
