package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PaymentChannelBillFetchJobHandler implements MangoJobHandler {

    public static final String JOB_CODE = "payment_channel_bill_fetch_yesterday";
    public static final String HANDLER_NAME = "paymentChannelBillFetchJobHandler";

    private final PaymentChannelBillFetchScheduleService scheduleService;
    private final ObjectMapper objectMapper;

    @Override
    public String appCode() {
        return "mango-monolith-app";
    }

    @Override
    public String serviceCode() {
        return "mango-monolith-app";
    }

    @Override
    public String workerGroup() {
        return "mango-monolith-app";
    }

    @Override
    public Set<String> supportedJobCodes() {
        return Set.of(JOB_CODE);
    }

    @Override
    public String handlerName() {
        return HANDLER_NAME;
    }

    @Override
    public MangoJobHandleResult handle(MangoJobHandleContext context) {
        LocalDate billDate = resolveBillDate(context);
        PaymentChannelBillFetchScheduleService.ScheduledBillFetchResult result =
                scheduleService.fetchScheduledChannelBills(billDate);
        String message = "通道账单定时拉取完成：账单日 " + result.billDate()
                + "，总数 " + result.totalCount()
                + "，成功 " + result.successCount()
                + "，跳过 " + result.skippedCount()
                + "，失败 " + result.failedCount();
        if (result.failedCount() > 0) {
            return MangoJobHandleResult.failed(message + "；" + String.join("；", result.messages()));
        }
        return MangoJobHandleResult.success(message);
    }

    private LocalDate resolveBillDate(MangoJobHandleContext context) {
        String parameter = context == null ? null : context.getParameter();
        if (!StringUtils.hasText(parameter)) {
            return LocalDate.now().minusDays(1);
        }
        try {
            JsonNode root = objectMapper.readTree(parameter);
            JsonNode value = root.get("billDate");
            if (value == null || !StringUtils.hasText(value.asText())) {
                return LocalDate.now().minusDays(1);
            }
            return LocalDate.parse(value.asText().trim());
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException("账单拉取任务参数 billDate 必须是 yyyy-MM-dd 格式", ex);
        }
    }
}
