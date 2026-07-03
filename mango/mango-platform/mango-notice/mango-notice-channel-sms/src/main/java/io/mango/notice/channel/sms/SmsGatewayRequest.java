package io.mango.notice.channel.sms;

record SmsGatewayRequest(String mobile, String signName, String templateCode, String templateParam,
                         AliyunSmsConfig config) {
}
