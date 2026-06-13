package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.RetryPaymentNotificationRecordCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentNotificationRecordVO;
import io.mango.payment.api.vo.PaymentNotificationStatusVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentNotificationRecordApi {

    R<PageResult<PaymentNotificationRecordVO>> pageNotificationRecords(@Valid PaymentConfigPageQuery query);

    R<PaymentNotificationRecordVO> detailNotificationRecord(@NotNull(message = "通知记录 ID 不能为空") Long id);

    R<List<PaymentNotificationStatusVO>> listNotificationStatuses();

    R<PaymentNotificationRecordVO> retryNotificationRecord(@Valid RetryPaymentNotificationRecordCommand command);

    R<Integer> deliverDueNotificationRecords(long limit);
}
