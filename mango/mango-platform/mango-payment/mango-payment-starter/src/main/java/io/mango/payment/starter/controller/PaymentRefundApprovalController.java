package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentRefundApprovalApi;
import io.mango.payment.api.command.CreatePaymentRefundApprovalCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentRefundApprovalStatusVO;
import io.mango.payment.api.vo.PaymentRefundApprovalVO;
import io.mango.payment.core.service.PaymentRefundApprovalService;
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
@RequestMapping("/payment/refund-approvals")
@RequiredArgsConstructor
@Tag(name = "退款审批", description = "后台退款审批单接口")
public class PaymentRefundApprovalController implements PaymentRefundApprovalApi {

    private final PaymentRefundApprovalService refundApprovalService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:list")
    @Operation(summary = "分页查询退款审批", description = "查询后台发起退款的审批单、审批状态和关联退款订单")
    public R<PageResult<PaymentRefundApprovalVO>> pageRefundApprovals(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(refundApprovalService.pageRefundApprovals(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:query")
    @Operation(summary = "查询退款审批详情", description = "按退款审批 ID 查询审批单、申请人、审核人和关联退款结果")
    public R<PaymentRefundApprovalVO> detailRefundApproval(@Parameter(description = "退款审批 ID", required = true) @RequestParam Long id) {
        return R.ok(refundApprovalService.detailRefundApproval(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:list")
    @Operation(summary = "查询退款审批状态选项", description = "返回退款审批后台筛选使用的状态契约")
    public R<List<PaymentRefundApprovalStatusVO>> listRefundApprovalStatuses() {
        return R.ok(refundApprovalService.listRefundApprovalStatuses());
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:create")
    @Operation(summary = "创建退款审批", description = "后台发起退款必须先创建审批单，只校验原支付订单、可退金额和幂等号，不直接生成退款成功状态")
    public R<PaymentRefundApprovalVO> createRefundApproval(@Valid @RequestBody CreatePaymentRefundApprovalCommand command) {
        return R.ok(refundApprovalService.createRefundApproval(command));
    }
}
