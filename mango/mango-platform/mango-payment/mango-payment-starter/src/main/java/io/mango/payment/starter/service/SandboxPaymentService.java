package io.mango.payment.starter.service;

import io.mango.common.result.Require;
import io.mango.payment.api.command.PaymentNotifyCommand;
import io.mango.payment.api.command.QueryPaymentOrderCommand;
import io.mango.payment.api.command.SandboxPaymentCommand;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.SandboxPaymentNotifyVO;
import io.mango.payment.channel.sandbox.SandboxPaymentChannelProvider;
import io.mango.payment.core.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SandboxPaymentService implements ISandboxPaymentService {

    private final IPaymentService paymentService;

    @Override
    public SandboxPaymentNotifyVO createPaymentNotify(SandboxPaymentCommand command) {
        PaymentOrderVO paymentOrder = paymentService.queryPaymentOrder(queryPaymentOrderCommand(command.getPaymentOrderId()));
        Require.notNull(paymentOrder, "支付单不存在");
        Require.isTrue(SandboxPaymentChannelProvider.CHANNEL_CODE.equals(paymentOrder.getChannelCode()),
                "支付单不是沙箱通道支付单");
        String channelOrderNo = SandboxPaymentChannelProvider.sandboxPaymentOrderNo(command.getPaymentOrderId());
        String signature = SandboxPaymentChannelProvider.signatureOf(
                command.getPaymentOrderId(), channelOrderNo, command.getSandboxEventId());
        PaymentNotifyCommand notifyCommand = new PaymentNotifyCommand();
        notifyCommand.setPaymentOrderId(command.getPaymentOrderId());
        notifyCommand.setChannelOrderNo(channelOrderNo);
        notifyCommand.setNotifyEventId(command.getSandboxEventId());
        notifyCommand.setSignature(signature);
        SandboxPaymentNotifyVO vo = new SandboxPaymentNotifyVO();
        vo.setChannelCode(SandboxPaymentChannelProvider.CHANNEL_CODE);
        vo.setPaymentOrderId(command.getPaymentOrderId());
        vo.setChannelOrderNo(channelOrderNo);
        vo.setNotifyEventId(command.getSandboxEventId());
        vo.setSignature(signature);
        vo.setNotifyCommand(notifyCommand);
        return vo;
    }

    @Override
    public PaymentOrderVO completePayment(SandboxPaymentCommand command) {
        SandboxPaymentNotifyVO notify = createPaymentNotify(command);
        boolean success = paymentService.paymentNotify(notify.getNotifyCommand());
        Require.isTrue(success, "沙箱支付回调验签失败");
        return paymentService.queryPaymentOrder(queryPaymentOrderCommand(command.getPaymentOrderId()));
    }

    private QueryPaymentOrderCommand queryPaymentOrderCommand(Long paymentOrderId) {
        QueryPaymentOrderCommand command = new QueryPaymentOrderCommand();
        command.setPaymentOrderId(paymentOrderId);
        return command;
    }
}
