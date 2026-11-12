package com.salesmanager.core.business.services.order;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.salesmanager.core.business.repositories.order.DailyOrderAggregate;
import com.salesmanager.core.business.repositories.order.OrderRepository;

@Service
public class DailyOrderSummaryService {
    private final OrderRepository repository;

    public DailyOrderSummaryService(OrderRepository repository) {
        this.repository = repository;
    }

    public static void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required (yyyy-MM-dd)");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) >= 366) {
            throw new IllegalArgumentException("Date range must not exceed 366 days inclusive");
        }
    }

    @Transactional(readOnly = true)
    public List<DailyOrderAggregate> summarize(Integer merchantId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        return repository.summarizeDaily(merchantId, Date.valueOf(startDate), Date.valueOf(endDate));
    }
}
