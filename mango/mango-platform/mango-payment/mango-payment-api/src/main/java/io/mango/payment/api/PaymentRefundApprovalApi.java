package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.CreatePaymentRefundApprovalCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentRefundApprovalStatusVO;
import io.mango.payment.api.vo.PaymentRefundApprovalVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentRefundApprovalApi {

    R<PageResult<PaymentRefundApprovalVO>> pageRefundApprovals(@Valid PaymentConfigPageQuery query);

    R<PaymentRefundApprovalVO> detailRefundApproval(@NotNull(message = "退款审批 ID 不能为空") Long id);

    R<List<PaymentRefundApprovalStatusVO>> listRefundApprovalStatuses();

    R<PaymentRefundApprovalVO> createRefundApproval(@Valid CreatePaymentRefundApprovalCommand command);
}
