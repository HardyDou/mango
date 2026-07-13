package io.mango.payment.core.service;

public interface IPaymentNotificationRecordService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentNotificationRecordVO> pageNotificationRecords(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentNotificationRecordVO detailNotificationRecord(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentNotificationStatusVO> listNotificationStatuses();
    io.mango.payment.api.vo.PaymentNotificationRecordVO retryNotificationRecord(
            io.mango.payment.api.command.RetryPaymentNotificationRecordCommand command);
    int deliverDueNotificationRecords(long limit);
}
