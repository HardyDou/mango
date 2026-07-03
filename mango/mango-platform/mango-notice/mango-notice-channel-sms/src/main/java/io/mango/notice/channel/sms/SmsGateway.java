package io.mango.notice.channel.sms;

interface SmsGateway {

    SmsGatewayResponse send(SmsGatewayRequest request);
}
