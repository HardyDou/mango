package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOperationAuditApi;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOperationAuditVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "mango-payment", contextId = "paymentOperationAuditFeignClient", path = "/payment/operation-audits")
public interface PaymentOperationAuditFeignClient extends PaymentOperationAuditApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentOperationAuditVO>> pageOperationAudits(@SpringQueryMap PaymentConfigPageQuery query);
}
