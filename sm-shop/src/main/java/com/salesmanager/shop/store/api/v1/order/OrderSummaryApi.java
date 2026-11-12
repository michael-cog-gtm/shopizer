package com.salesmanager.shop.store.api.v1.order;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.salesmanager.core.business.services.order.DailyOrderSummaryService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.order.ReadableDailyOrderSummary;
import com.salesmanager.shop.store.api.exception.RestApiException;
import com.salesmanager.shop.store.api.exception.ErrorEntity;
import com.salesmanager.shop.utils.AuthorizationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/private/orders/summary")
@Api(tags = { "Ordering api (Order Flow Api)" })
public class OrderSummaryApi {
    private final DailyOrderSummaryService service;
    private final AuthorizationUtils authorizationUtils;

    public OrderSummaryApi(DailyOrderSummaryService service, AuthorizationUtils authorizationUtils) {
        this.service = service;
        this.authorizationUtils = authorizationUtils;
    }

    @RequestMapping(value = "/daily", method = RequestMethod.GET)
    @ApiResponses({ @ApiResponse(code = 400, message = "Missing or invalid dates, reversed range, or range exceeding 366 days",
            response = ErrorEntity.class) })
    @ApiOperation(value = "Daily order counts and totals by status and currency",
            notes = "startDate and endDate are required ISO yyyy-MM-dd dates, inclusive, at most 366 days. "
                    + "Days are DATE_PURCHASED calendar dates as stored (DATE only, no time zone conversion). "
                    + "Every day is returned, including empty days. Totals sum ORDER_TOTAL separately per currency.")
    @ApiImplicitParams({ @ApiImplicitParam(name = "store", paramType = "query", dataType = "string", defaultValue = "DEFAULT"),
            @ApiImplicitParam(name = "lang", paramType = "query", dataType = "string", defaultValue = "en"),
            @ApiImplicitParam(name = "startDate", paramType = "query", dataType = "string", required = true),
            @ApiImplicitParam(name = "endDate", paramType = "query", dataType = "string", required = true) })
    public ReadableDailyOrderSummary daily(@ApiIgnore MerchantStore merchantStore,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        String user = authorizationUtils.authenticatedUser();
        authorizationUtils.authorizeUser(user, Stream.of(Constants.GROUP_SUPERADMIN, Constants.GROUP_ADMIN,
                Constants.GROUP_ADMIN_ORDER, Constants.GROUP_ADMIN_RETAIL).collect(Collectors.toList()), merchantStore);
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        try {
            DailyOrderSummaryService.validateRange(start, end);
        } catch (IllegalArgumentException exception) {
            throw new RestApiException("400", exception.getMessage());
        }
        return DailyOrderSummaryMapper.map(merchantStore.getCode(), start, end,
                service.summarize(merchantStore.getId(), start, end));
    }

    private LocalDate parseDate(String value, String name) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new RestApiException("400", name + " is required and must use yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new RestApiException("400", name + " must be a valid date (yyyy-MM-dd)");
        }
    }
}
