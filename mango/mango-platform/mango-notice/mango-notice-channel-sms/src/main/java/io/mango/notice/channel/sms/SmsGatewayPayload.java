package io.mango.notice.channel.sms;

final class SmsGatewayPayload {

    private final String mobile;
    private final String signName;
    private final String templateCode;
    private final String templateParam;
    private final SmsProviderConfig config;

    SmsGatewayPayload(String mobile, String signName, String templateCode, String templateParam,
            SmsProviderConfig config) {
        this.mobile = mobile;
        this.signName = signName;
        this.templateCode = templateCode;
        this.templateParam = templateParam;
        this.config = config;
    }

    String mobile() {
        return mobile;
    }

    String signName() {
        return signName;
    }

    String templateCode() {
        return templateCode;
    }

    String templateParam() {
        return templateParam;
    }

    SmsProviderConfig config() {
        return config;
    }
}
