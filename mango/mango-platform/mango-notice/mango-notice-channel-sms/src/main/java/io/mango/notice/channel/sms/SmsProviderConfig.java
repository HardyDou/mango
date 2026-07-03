package io.mango.notice.channel.sms;

interface SmsProviderConfig {

    String providerCode();

    String signName();

    String templateCode();
}
