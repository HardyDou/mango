package io.mango.payment.core.service;

import io.mango.payment.api.vo.PaymentManageDomainVO;
import io.mango.payment.api.vo.PaymentManageItemVO;
import io.mango.payment.api.vo.PaymentMethodVO;
import io.mango.payment.api.vo.PaymentTenantCashierVO;
import java.util.List;

public interface IPaymentManagementService {

    List<PaymentManageDomainVO> listDomains();

    List<PaymentManageItemVO> listItems(String domain);

    List<PaymentTenantCashierVO> listTenantCashiers();

    List<PaymentMethodVO> listSandboxMethods();
}
