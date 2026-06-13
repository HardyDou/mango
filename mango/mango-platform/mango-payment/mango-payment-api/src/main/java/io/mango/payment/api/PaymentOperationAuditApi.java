package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOperationAuditVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentOperationAuditApi {

    R<PageResult<PaymentOperationAuditVO>> pageOperationAudits(@Valid PaymentConfigPageQuery query);
}
