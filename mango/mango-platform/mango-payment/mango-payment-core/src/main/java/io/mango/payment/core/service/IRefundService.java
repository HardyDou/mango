package io.mango.payment.core.service;

import io.mango.payment.api.command.QueryRefundOrderCommand;
import io.mango.payment.api.command.RefreshRefundStatusCommand;
import io.mango.payment.api.command.RefundCommand;
import io.mango.payment.api.vo.RefundOrderVO;

public interface IRefundService {

    RefundOrderVO refund(RefundCommand command);

    RefundOrderVO queryRefundOrder(QueryRefundOrderCommand command);

    RefundOrderVO refreshRefundStatus(RefreshRefundStatusCommand command);
}
