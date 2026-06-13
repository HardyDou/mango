package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOperationAuditApi;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOperationAuditVO;
import io.mango.payment.core.service.PaymentOperationAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/operation-audits")
@RequiredArgsConstructor
@Tag(name = "支付操作审计", description = "支付域关键操作审计接口")
public class PaymentOperationAuditController implements PaymentOperationAuditApi {

    private final PaymentOperationAuditService auditService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:operation-audit:list")
    @Operation(summary = "分页查询操作审计", description = "查询支付域关键配置变更、资金相关人工操作和审批处理的审计记录")
    public R<PageResult<PaymentOperationAuditVO>> pageOperationAudits(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(auditService.pageOperationAudits(query));
    }
}
