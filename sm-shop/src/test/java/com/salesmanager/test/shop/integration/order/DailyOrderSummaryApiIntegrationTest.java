package com.salesmanager.test.shop.integration.order;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.salesmanager.core.business.repositories.order.OrderRepository;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.currency.CurrencyService;
import com.salesmanager.core.business.services.user.GroupService;
import com.salesmanager.core.business.services.user.UserService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.common.Billing;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.orderstatus.OrderStatus;
import com.salesmanager.core.model.reference.currency.Currency;
import com.salesmanager.core.model.user.User;
import com.salesmanager.test.shop.common.ServicesTestSupport;

class DailyOrderSummaryApiIntegrationTest extends ServicesTestSupport {
    @Autowired private OrderRepository orders;
    @Autowired private MerchantStoreService stores;
    @Autowired private CurrencyService currencies;
    @Autowired private UserService users;
    @Autowired private GroupService groups;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void persistedOrdersAreAggregatedAndAuthorizedByStore() throws Exception {
        MerchantStore a = stores.getByCode("DEFAULT");
        MerchantStore b = new MerchantStore();
        b.setCode("DAILY-SUMMARY-B");
        b.setStorename("Daily Summary B");
        b.setStoreEmailAddress("daily-b@example.com");
        b.setCountry(a.getCountry());
        b.setDefaultLanguage(a.getDefaultLanguage());
        b.setCurrency(a.getCurrency());
        stores.create(b);
        User admin = new User("daily-b@example.com", passwordEncoder.encode("password"), "daily-b@example.com");
        admin.setMerchantStore(b);
        admin.setDefaultLanguage(a.getDefaultLanguage());
        admin.getGroups().add(groups.findByName("ADMIN"));
        users.saveOrUpdate(admin);
        List<Order> seeded = new ArrayList<>();
        try {
            Currency usd = currencies.getByCode("USD");
            Currency cad = currencies.getByCode("CAD");
            assertNotNull(usd);
            assertNotNull(cad);
            seeded.add(seed(a, "2040-01-01", OrderStatus.ORDERED, usd, "10.25"));
            seeded.add(seed(a, "2040-01-01", OrderStatus.ORDERED, usd, "2.75"));
            seeded.add(seed(a, "2040-01-01", OrderStatus.DELIVERED, usd, "4.00"));
            seeded.add(seed(a, "2040-01-03", OrderStatus.ORDERED, cad, "9.50"));
            seeded.add(seed(b, "2040-01-01", OrderStatus.ORDERED, usd, "100.00"));
            seeded.add(seed(a, "2039-12-31", OrderStatus.ORDERED, usd, "999.00"));
            seeded.add(seed(a, "2040-01-04", OrderStatus.ORDERED, usd, "999.00"));
            HttpHeaders superadmin = getHeader();
            String range = "&startDate=2040-01-01&endDate=2040-01-03";
            ResponseEntity<JsonNode> response = get("DEFAULT" + range, superadmin);
            assertEquals(200, response.getStatusCodeValue(), response.toString());
            JsonNode body = response.getBody();
            assertEquals("DEFAULT", body.path("store").asText());
            assertEquals("2040-01-01", body.path("startDate").asText());
            assertEquals("2040-01-03", body.path("endDate").asText());
            assertEquals(3, body.path("days").size());
            assertEquals(3, body.path("days").get(0).path("orderCount").asInt());
            assertEquals(0, body.path("days").get(1).path("orderCount").asInt());
            assertEquals(0, body.path("days").get(1).path("breakdown").size());
            assertEquals(1, body.path("days").get(2).path("orderCount").asInt());
            assertEquals(2, body.path("totals").size());
            assertTotal(body.path("totals"), "USD", 3, "17.00");
            assertTotal(body.path("totals"), "CAD", 1, "9.50");
            assertEquals(3, body.path("breakdown").size());
            JsonNode orderedUsd = findStatus(body.path("breakdown"), "ORDERED", "USD");
            assertNotNull(orderedUsd);
            assertEquals(2, orderedUsd.path("orderCount").asInt());
            assertEquals(0, new BigDecimal("13.00").compareTo(orderedUsd.path("total").decimalValue()));
            System.out.println("DAILY_SUMMARY_SAMPLE=" + testRestTemplate.exchange(
                    "/api/v1/private/orders/summary/daily?store=DEFAULT" + range, HttpMethod.GET,
                    new HttpEntity<>(superadmin), String.class).getBody());
            JsonNode empty = get("DEFAULT&startDate=2040-01-02&endDate=2040-01-02", superadmin).getBody();
            assertEquals(1, empty.path("days").size());
            assertEquals(0, empty.path("days").get(0).path("orderCount").asInt());
            assertEquals(0, empty.path("breakdown").size());
            assertEquals(0, empty.path("totals").size());

            ResponseEntity<JsonNode> swagger = testRestTemplate.getForEntity("/v2/api-docs", JsonNode.class);
            assertEquals(200, swagger.getStatusCodeValue());
            JsonNode operation = swagger.getBody().path("paths").path("/api/v1/private/orders/summary/daily").path("get");
            assertTrue(operation.path("description").asText().contains("DATE_PURCHASED"));
            assertTrue(operation.path("responses").has("200"));
            assertEquals(4, operation.path("parameters").size());
            for (JsonNode parameter : operation.path("parameters")) {
                assertEquals("query", parameter.path("in").asText());
                if (parameter.path("name").asText().endsWith("Date")) {
                    assertTrue(parameter.path("required").asBoolean());
                }
            }
            assertEquals(200, testRestTemplate.getForEntity("/swagger-ui.html", String.class).getStatusCodeValue());

            HttpHeaders storeAdmin = getHeader("daily-b@example.com", "password");
            assertEquals(401, get("DEFAULT" + range, storeAdmin).getStatusCodeValue());
            ResponseEntity<JsonNode> own = get(b.getCode() + range, storeAdmin);
            assertEquals(200, own.getStatusCodeValue(), own.toString());
            assertTotal(own.getBody().path("totals"), "USD", 1, "100.00");
            assertEquals(1, own.getBody().path("totals").size());
            assertEquals(401, get("DEFAULT" + range, new HttpHeaders()).getStatusCodeValue());
            for (String invalid : new String[] { "", "&startDate=2040-01-01", "&endDate=2040-01-01",
                    "&startDate=nope&endDate=2040-01-01", "&startDate=2040-02-30&endDate=2040-03-01",
                    "&startDate=2040-01-03&endDate=2040-01-01", "&startDate=2040-01-01&endDate=2041-01-01" }) {
                ResponseEntity<JsonNode> bad = get("DEFAULT" + invalid, superadmin);
                assertEquals(400, bad.getStatusCodeValue(), bad.toString());
                assertFalse(bad.getBody().path("message").asText().isEmpty());
            }
            assertEquals(200, testRestTemplate.exchange("/api/v1/private/orders", HttpMethod.GET,
                    new HttpEntity<>(superadmin), String.class).getStatusCodeValue());
        } finally {
            orders.deleteAll(seeded);
            users.delete(admin);
            stores.delete(b);
        }
    }

    private ResponseEntity<JsonNode> get(String query, HttpHeaders headers) {
        return testRestTemplate.exchange("/api/v1/private/orders/summary/daily?store=" + query,
                HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    private Order seed(MerchantStore store, String day, OrderStatus status, Currency currency, String total) {
        Order order = new Order();
        order.setMerchant(store);
        order.setDatePurchased(Date.valueOf(day));
        order.setStatus(status);
        order.setCurrency(currency);
        order.setTotal(new BigDecimal(total));
        order.setCustomerEmailAddress("daily-customer@example.com");
        Billing billing = new Billing();
        billing.setFirstName("Daily");
        billing.setLastName("Summary");
        billing.setCountry(store.getCountry());
        order.setBilling(billing);
        return orders.saveAndFlush(order);
    }

    private void assertTotal(JsonNode entries, String currency, int count, String total) {
        for (JsonNode entry : entries) {
            if (currency.equals(entry.path("currency").asText())) {
                assertEquals(count, entry.path("orderCount").asInt());
                assertEquals(0, new BigDecimal(total).compareTo(entry.path("total").decimalValue()));
                return;
            }
        }
        fail("Missing currency " + currency);
    }

    private JsonNode findStatus(JsonNode entries, String status, String currency) {
        for (JsonNode entry : entries) {
            if (status.equals(entry.path("status").asText()) && currency.equals(entry.path("currency").asText())) {
                return entry;
            }
        }
        return null;
    }
}
