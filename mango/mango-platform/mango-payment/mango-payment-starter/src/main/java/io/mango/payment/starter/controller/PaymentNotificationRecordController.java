package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentNotificationRecordApi;
import io.mango.payment.api.command.RetryPaymentNotificationRecordCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentNotificationRecordVO;
import io.mango.payment.api.vo.PaymentNotificationStatusVO;
import io.mango.payment.core.service.PaymentNotificationRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/payment/notification-records")
@RequiredArgsConstructor
@Tag(name = "通知记录", description = "支付结果通知业务系统的发送和补偿接口")
public class PaymentNotificationRecordController implements PaymentNotificationRecordApi {

    private final PaymentNotificationRecordService notificationRecordService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:list")
    @Operation(summary = "分页查询通知记录", description = "查询支付或退款结果通知业务系统的发送状态、响应和重试信息")
    public R<PageResult<PaymentNotificationRecordVO>> pageNotificationRecords(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(notificationRecordService.pageNotificationRecords(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:query")
    @Operation(summary = "查询通知记录详情", description = "按通知记录 ID 查询通知目标、响应、重试和人工补偿信息")
    public R<PaymentNotificationRecordVO> detailNotificationRecord(@Parameter(description = "通知记录 ID", required = true) @RequestParam Long id) {
        return R.ok(notificationRecordService.detailNotificationRecord(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:list")
    @Operation(summary = "查询通知状态选项", description = "返回通知记录后台筛选使用的通知状态契约")
    public R<List<PaymentNotificationStatusVO>> listNotificationStatuses() {
        return R.ok(notificationRecordService.listNotificationStatuses());
    }

    @Override
    @PostMapping("/retry")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:retry")
    @Operation(summary = "人工重推通知记录", description = "登记失败通知的人工补偿重推，仅重推已有支付或退款结果，不改变资金状态")
    public R<PaymentNotificationRecordVO> retryNotificationRecord(@Valid @RequestBody RetryPaymentNotificationRecordCommand command) {
        return R.ok(notificationRecordService.retryNotificationRecord(command));
    }

    @Override
    @PostMapping("/deliver-due")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:deliver-due")
    @Operation(summary = "投递到期通知记录", description = "人工触发投递当前租户已到计划时间的支付或退款通知记录，不改变资金状态并记录操作审计")
    public R<Integer> deliverDueNotificationRecords(@Parameter(description = "本次最多投递条数，1-100") @RequestParam(defaultValue = "20") long limit) {
        return R.ok(notificationRecordService.deliverDueNotificationRecords(limit));
    }
}
