package io.mango.notice.channel.sms;

interface SmsGateway {

    SmsGatewayResult send(SmsGatewayPayload payload);
}
