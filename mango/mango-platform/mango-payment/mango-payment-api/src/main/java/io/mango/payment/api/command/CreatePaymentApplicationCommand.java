package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建支付接入应用命令")
public class CreatePaymentApplicationCommand extends SavePaymentApplicationCommand {

    private static final long serialVersionUID = 1L;
}
