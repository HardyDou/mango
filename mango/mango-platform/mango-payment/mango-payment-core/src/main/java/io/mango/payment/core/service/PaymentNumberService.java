package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.numgen.api.NumgenApi;
import io.mango.numgen.api.command.NumgenNextCommand;
import io.mango.payment.api.PaymentCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentNumberService {

    public static final String PAY_BIZ_ORDER_NO = "PAY_BIZ_ORDER_NO";
    public static final String PAY_ORDER_NO = "PAY_ORDER_NO";
    public static final String PAY_REFUND_ORDER_NO = "PAY_REFUND_ORDER_NO";
    public static final String PAY_BIZ_REFUND_NO = "PAY_BIZ_REFUND_NO";
    public static final String PAY_REFUND_APPROVAL_NO = "PAY_REFUND_APPROVAL_NO";
    public static final String PAY_FLOW_NO = "PAY_FLOW_NO";
    public static final String PAY_REFUND_FLOW_NO = "PAY_REFUND_FLOW_NO";
    public static final String PAY_FEE_FLOW_NO = "PAY_FEE_FLOW_NO";
    public static final String PAY_ADJUST_FLOW_NO = "PAY_ADJUST_FLOW_NO";
    public static final String PAY_NOTIFY_NO = "PAY_NOTIFY_NO";
    public static final String PAY_RECON_BATCH_NO = "PAY_RECON_BATCH_NO";
    public static final String PAY_DIFF_NO = "PAY_DIFF_NO";
    public static final String PAY_QUERY_NO = "PAY_QUERY_NO";
    public static final String PAY_REFUND_QUERY_NO = "PAY_REFUND_QUERY_NO";
    public static final String PAY_EXCEPTION_NO = "PAY_EXCEPTION_NO";
    public static final String PAY_OFFLINE_COLLECTION_NO = "PAY_OFFLINE_COLLECTION_NO";
    public static final String PAY_OFFLINE_REFUND_NO = "PAY_OFFLINE_REFUND_NO";
    public static final String PAY_OFFLINE_BANK_BATCH_NO = "PAY_OFFLINE_BANK_BATCH_NO";
    public static final String PAY_MANGO_VIRTUAL_NO = "PAY_MANGO_VIRTUAL_NO";
    public static final String PAY_MANGO_SCENARIO_NO = "PAY_MANGO_SCENARIO_NO";

    private final NumgenApi numgenApi;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String genKey) {
        Require.notBlank(genKey, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "支付编号规则键不能为空");
        NumgenNextCommand command = new NumgenNextCommand();
        command.setGenKey(genKey);
        command.setParams(Map.of("bizKey", genKey));
        R<String> result = numgenApi.nextValue(command);
        Require.isTrue(result != null && result.isSuccess(),
                PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(),
                result == null ? "支付编号生成失败" : result.getMsg());
        return result.getData();
    }
}
