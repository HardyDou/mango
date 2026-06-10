package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentMethodCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentMethodCategoryVO;
import io.mango.payment.api.vo.PaymentMethodVO;

import java.util.List;

public interface IPaymentMethodService {

    R<PageResult<PaymentMethodVO>> pageMethods(PaymentConfigPageQuery query);

    R<List<PaymentMethodCategoryVO>> listMethodCategories();

    R<PaymentMethodVO> detailMethod(Long id);

    R<Long> createMethod(SavePaymentMethodCommand command);

    R<Boolean> updateMethod(SavePaymentMethodCommand command);

    R<Boolean> deleteMethod(Long id);
}
