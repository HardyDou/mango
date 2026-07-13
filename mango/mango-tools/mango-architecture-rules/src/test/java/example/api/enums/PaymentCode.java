package example.api.enums;

import io.mango.common.result.BizCode;

public enum PaymentCode implements BizCode {
    INVALID;

    @Override
    public int getCode() {
        return 400;
    }

    @Override
    public String getMessage() {
        return "支付参数无效";
    }
}
