package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentMethodCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentMethodCategoryVO;
import io.mango.payment.api.vo.PaymentMethodVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentMethodApi {

    R<PageResult<PaymentMethodVO>> pageMethods(@Valid PaymentConfigPageQuery query);

    R<java.util.List<PaymentMethodCategoryVO>> listMethodCategories();

    R<PaymentMethodVO> detailMethod(@NotNull(message = "支付方式 ID 不能为空") Long id);

    R<Long> createMethod(@Valid SavePaymentMethodCommand command);

    R<Boolean> updateMethod(@Valid SavePaymentMethodCommand command);

    R<Boolean> deleteMethod(@NotNull(message = "支付方式 ID 不能为空") Long id);
}
