package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.vo.PaymentManageDomainVO;
import io.mango.payment.api.vo.PaymentManageItemVO;
import io.mango.payment.api.vo.PaymentMethodVO;
import io.mango.payment.api.vo.PaymentTenantCashierVO;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;

/**
 * 支付后台管理能力接口。
 */
@Validated
public interface PaymentManagementApi {

    /**
     * 查询后台管理域。
     *
     * @return 后台管理域列表
     */
    R<List<PaymentManageDomainVO>> listDomains();

    /**
     * 查询后台管理配置项。
     *
     * @param domain 管理域编码
     * @return 配置项列表
     */
    R<List<PaymentManageItemVO>> listItems(@NotBlank(message = "管理域不能为空") String domain);

    /**
     * 查询租户收银台。
     *
     * @return 租户收银台列表
     */
    R<List<PaymentTenantCashierVO>> listTenantCashiers();

    /**
     * 查询沙箱支付方式。
     *
     * @return 沙箱支付方式列表
     */
    R<List<PaymentMethodVO>> listSandboxMethods();
}
