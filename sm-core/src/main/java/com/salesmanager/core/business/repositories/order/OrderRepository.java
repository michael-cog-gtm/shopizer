package com.salesmanager.core.business.repositories.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.salesmanager.core.model.order.Order;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    @Query("select o.datePurchased as day, o.status as status, c.code as currency, "
            + "count(o) as orderCount, sum(o.total) as total from Order o join o.currency c "
            + "where o.merchant.id = ?1 and o.datePurchased >= ?2 and o.datePurchased <= ?3 "
            + "group by o.datePurchased, o.status, c.code order by o.datePurchased, o.status, c.code")
    java.util.List<DailyOrderAggregate> summarizeDaily(Integer merchantId, java.util.Date startDate,
            java.util.Date endDate);

    @Query("select o from Order o join fetch o.merchant om "
    		+ "join fetch o.orderProducts op "
    		+ "left join fetch o.delivery od left join fetch od.country left join fetch od.zone "
    		+ "left join fetch o.billing ob left join fetch ob.country left join fetch ob.zone "
    		+ "left join fetch o.orderAttributes oa "
    		+ "join fetch o.orderTotal ot left "
    		+ "join fetch o.orderHistory oh left "
    		+ "join fetch op.downloads opd left "
    		+ "join fetch op.orderAttributes opa "
    		+ "left join fetch op.prices opp where o.id = ?1 and om.id = ?2")
	Order findOne(Long id, Integer merchantId);
    
}
