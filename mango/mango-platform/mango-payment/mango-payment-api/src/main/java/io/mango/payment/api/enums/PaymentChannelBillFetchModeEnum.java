package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 通道账单获取方式。
 */
public enum PaymentChannelBillFetchModeEnum {

    MANUAL("MANUAL", "手动上传"),
    FTP("FTP", "FTP 拉取"),
    FTPS("FTPS", "FTPS 拉取"),
    HTTP("HTTP", "HTTP 接口");

    private final String code;
    private final String label;

    PaymentChannelBillFetchModeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean contains(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static String labelOf(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .map(PaymentChannelBillFetchModeEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentChannelBillFetchModeEnum> options() {
        return List.of(values());
    }
}
