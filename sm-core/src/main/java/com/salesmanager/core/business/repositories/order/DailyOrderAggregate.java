package com.salesmanager.core.business.repositories.order;

import java.math.BigDecimal;
import java.util.Date;
import com.salesmanager.core.model.order.orderstatus.OrderStatus;

public interface DailyOrderAggregate {
    Date getDay();
    OrderStatus getStatus();
    String getCurrency();
    Long getOrderCount();
    BigDecimal getTotal();
}
