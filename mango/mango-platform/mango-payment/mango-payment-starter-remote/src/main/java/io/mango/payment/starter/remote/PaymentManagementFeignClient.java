package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentManagementApi;
import io.mango.payment.api.vo.PaymentManageDomainVO;
import io.mango.payment.api.vo.PaymentManageItemVO;
import io.mango.payment.api.vo.PaymentMethodVO;
import io.mango.payment.api.vo.PaymentTenantCashierVO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", path = "/payment/management")
public interface PaymentManagementFeignClient extends PaymentManagementApi {

    @Override
    @GetMapping("/domains")
    R<List<PaymentManageDomainVO>> listDomains();

    @Override
    @GetMapping("/items")
    R<List<PaymentManageItemVO>> listItems(@RequestParam String domain);

    @Override
    @GetMapping("/tenant-cashiers")
    R<List<PaymentTenantCashierVO>> listTenantCashiers();

    @Override
    @GetMapping("/sandbox-methods")
    R<List<PaymentMethodVO>> listSandboxMethods();
}
