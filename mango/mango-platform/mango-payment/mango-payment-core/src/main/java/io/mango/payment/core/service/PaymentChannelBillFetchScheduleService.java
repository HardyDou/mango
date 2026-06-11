package io.mango.payment.core.service;

import io.mango.payment.api.command.FetchPaymentChannelBillCommand;
import io.mango.payment.core.entity.PaymentChannelBillSourceEntity;
import io.mango.payment.core.mapper.PaymentChannelBillFetchBatchMapper;
import io.mango.payment.core.mapper.PaymentChannelBillSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentChannelBillFetchScheduleService {

    private final PaymentChannelBillSourceMapper billSourceMapper;
    private final PaymentChannelBillFetchBatchMapper billFetchBatchMapper;
    private final PaymentReconciliationService reconciliationService;

    public ScheduledBillFetchResult fetchScheduledChannelBills(LocalDate billDate) {
        LocalDate resolvedBillDate = billDate == null ? LocalDate.now().minusDays(1) : billDate;
        Long tenantId = PaymentContextSupport.currentTenantId();
        List<PaymentChannelBillSourceEntity> sources = billSourceMapper.selectEnabledAutomaticSources(tenantId);
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<String> messages = new ArrayList<>();
        for (PaymentChannelBillSourceEntity source : sources) {
            long successfulFetch = billFetchBatchMapper.countSuccessfulFetch(tenantId, source.getId(), resolvedBillDate);
            if (successfulFetch > 0) {
                skippedCount++;
                messages.add("已跳过：" + source.getChannelCode() + "/" + source.getFetchMode() + "/" + resolvedBillDate);
                continue;
            }
            try {
                reconciliationService.fetchChannelBill(fetchCommand(source, resolvedBillDate));
                successCount++;
            } catch (RuntimeException ex) {
                failedCount++;
                messages.add("失败：" + source.getChannelCode() + "/" + source.getFetchMode()
                        + "，" + truncate(ex.getMessage(), 200));
            }
        }
        return new ScheduledBillFetchResult(resolvedBillDate, sources.size(), successCount, failedCount, skippedCount, messages);
    }

    private FetchPaymentChannelBillCommand fetchCommand(PaymentChannelBillSourceEntity source, LocalDate billDate) {
        FetchPaymentChannelBillCommand command = new FetchPaymentChannelBillCommand();
        command.setSourceId(source.getId());
        command.setBillDate(billDate);
        return command;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record ScheduledBillFetchResult(
            LocalDate billDate,
            int totalCount,
            int successCount,
            int failedCount,
            int skippedCount,
            List<String> messages
    ) {
    }
}
