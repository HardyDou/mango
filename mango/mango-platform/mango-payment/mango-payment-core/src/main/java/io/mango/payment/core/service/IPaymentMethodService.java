package io.mango.payment.core.service;

import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentMethodCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentMethodCategoryVO;
import io.mango.payment.api.vo.PaymentMethodVO;

import java.util.List;

public interface IPaymentMethodService {

    PageResult<PaymentMethodVO> pageMethods(PaymentConfigPageQuery query);

    List<PaymentMethodCategoryVO> listMethodCategories();

    PaymentMethodVO detailMethod(Long id);

    Long createMethod(SavePaymentMethodCommand command);

    Boolean updateMethod(SavePaymentMethodCommand command);

    Boolean deleteMethod(Long id);
}
