package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFuiouPayMigrationTest {

    private static final Path CHANNEL_SEED_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V90__payment_fuiou_pay_channel_seed.sql");
    private static final Path CONTRACT_CONFIGURATION_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V91__payment_fuiou_channel_contract_configuration.sql");
    private static final Path CONTRACT_CONFIGURATION_CLEANUP_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V92__payment_fuiou_contract_configuration_cleanup.sql");
    private static final Path CONFIG_DISPLAY_CLEANUP_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V93__payment_fuiou_config_display_cleanup.sql");
    private static final Path GATEWAY_CAPABILITY_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V94__payment_fuiou_gateway_capabilities.sql");
    private static final Path CALLBACK_DOMAIN_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V96__payment_fuiou_callback_domain.sql");
    private static final Path SCANPAY_PUBLIC_KEY_FIX_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V97__payment_fuiou_scanpay_public_key_fix.sql");
    private static final Path CALLBACK_ROUTE_UNIFICATION_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V98__payment_channel_callback_route_unification.sql");
    private static final Path CALLBACK_HTTPS_1443_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V99__payment_fuiou_callback_https_1443.sql");
    private static final Path GATEWAY_PAGE_NOTIFY_URL_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V100__payment_fuiou_gateway_page_notify_url.sql");
    private static final Path PUBLIC_DEMO_PRIVATE_KEY_RESTORE_MIGRATION = Path.of(
            "src/main/resources/db/migration/payment/V101__payment_fuiou_public_demo_private_key_restore.sql");

    @Test
    @DisplayName("migration should move fuiou from legacy seed to business signing configuration")
    void migration_movesFuiouFromLegacySeedToBusinessSigningConfiguration() throws Exception {
        String seedSql = Files.readString(CHANNEL_SEED_MIGRATION);
        String configurationSql = Files.readString(CONTRACT_CONFIGURATION_MIGRATION);
        String cleanupSql = Files.readString(CONTRACT_CONFIGURATION_CLEANUP_MIGRATION);
        String displayCleanupSql = Files.readString(CONFIG_DISPLAY_CLEANUP_MIGRATION);
        String gatewayCapabilitySql = Files.readString(GATEWAY_CAPABILITY_MIGRATION);
        String callbackDomainSql = Files.readString(CALLBACK_DOMAIN_MIGRATION);
        String scanpayPublicKeyFixSql = Files.readString(SCANPAY_PUBLIC_KEY_FIX_MIGRATION);
        String callbackRouteUnificationSql = Files.readString(CALLBACK_ROUTE_UNIFICATION_MIGRATION);
        String callbackHttps1443Sql = Files.readString(CALLBACK_HTTPS_1443_MIGRATION);
        String gatewayPageNotifyUrlSql = Files.readString(GATEWAY_PAGE_NOTIFY_URL_MIGRATION);
        String publicDemoPrivateKeyRestoreSql = Files.readString(PUBLIC_DEMO_PRIVATE_KEY_RESTORE_MIGRATION);

        assertFuiouSeedDoesNotDeclareBillCapability(seedSql);
        assertFuiouScanpayConfiguration(configurationSql);
        assertFuiouScanpayConfiguration(cleanupSql);
        assertFuiouDisplayConfiguration(displayCleanupSql);
        assertFuiouGatewayConfiguration(gatewayCapabilitySql);
        assertFuiouCallbackDomain(callbackDomainSql);
        assertFuiouScanpayPublicKeyFix(scanpayPublicKeyFixSql);
        assertFuiouCallbackRouteUnification(callbackRouteUnificationSql);
        assertFuiouCallbackHttps1443(callbackHttps1443Sql);
        assertFuiouGatewayPageNotifyUrl(gatewayPageNotifyUrlSql);
        assertFuiouPublicDemoPrivateKeyRestore(publicDemoPrivateKeyRestoreSql);
        assertThat(seedSql)
                .contains("\"field\":\"termId\"")
                .contains("\"field\":\"termIp\"");
        assertThat(cleanupSql)
                .contains("JSON_REMOVE")
                .contains("'$.termId'")
                .contains("'$.termIp'");
    }

    private void assertFuiouCallbackRouteUnification(String sql) {
        assertThat(sql)
                .contains("'/api/payment/channel-callbacks/fuiou'")
                .contains("'/api/payment/channel-callbacks/fuiou_pay'")
                .contains("`channel_code` = 'FUIOU_PAY'");
    }

    private void assertFuiouCallbackHttps1443(String sql) {
        assertThat(sql)
                .contains("No-op by design")
                .contains("not overwritten by formal Flyway migration")
                .doesNotContain("UPDATE `payment_channel_contract`")
                .doesNotContain("JSON_MERGE_PATCH")
                .doesNotContain("27.185.20.146")
                .doesNotContain("douxy.inner.yunxinbaokeji.com");
    }

    private void assertFuiouCallbackDomain(String sql) {
        assertThat(sql)
                .contains("No-op by design")
                .contains("not overwritten by formal Flyway migration")
                .doesNotContain("UPDATE `payment_channel_contract`")
                .doesNotContain("JSON_MERGE_PATCH")
                .doesNotContain("27.185.20.146")
                .doesNotContain("douxy.inner.yunxinbaokeji.com");
    }

    private void assertFuiouGatewayPageNotifyUrl(String sql) {
        assertThat(sql)
                .contains("No-op by design")
                .contains("not overwritten by formal Flyway migration")
                .doesNotContain("UPDATE `payment_channel_contract`")
                .doesNotContain("JSON_MERGE_PATCH")
                .doesNotContain("douxy.inner.yunxinbaokeji.com");
    }

    private void assertFuiouPublicDemoPrivateKeyRestore(String sql) {
        assertThat(sql)
                .contains("public Fuiou demo merchant signing key")
                .contains("JSON_SET")
                .contains("'$.privateKey'")
                .contains("'FUIOU_PAY_MANGO_TECH'")
                .contains("'0002900F0370542'")
                .contains("JSON_UNQUOTE(JSON_EXTRACT(`config_values_json`, '$.privateKey')) = ''")
                .contains("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJc")
                .doesNotContain("gatewayMerchantKey");
    }

    private void assertFuiouScanpayConfiguration(String sql) {
        assertThat(sql)
                .contains("'FUIOU_PAY'")
                .contains("\"name\":\"privateKey\"")
                .contains("\"name\":\"fuiouPublicKey\"")
                .contains("\"masked\":false,\"sort\":21")
                .contains("\"name\":\"gatewayBaseUrl\"")
                .contains("报文终端号由系统按线上收款接口规则处理")
                .contains("'fuiouPublicKey', 'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDz2fCOYaaU6sztFql4cOmiFRq2LRk1XuGfrJnMFa09QMXMXOEn9YNYC44zV1AE/q9b0BKGbM74YPoge/7qsW+Heao76Drv6HujP+rXLFbsXT5f9rcID2GCzDc+DXjb+NfwSa8vS9KJ3dau2xm87zpjdQ9zER6VH4UcZTgj7LbzgwIDAQAB'")
                .contains("PERSONAL_WECHAT_QR")
                .contains("0.0020000000")
                .contains("PERSONAL_ALIPAY_QR")
                .contains("富友账单获取协议未完成适配前不声明通道账单能力")
                .doesNotContain("`bill_fetch_modes` = 'MANUAL,FTP,FTPS,HTTP'")
                .doesNotContain("`bill_fetch_modes` = 'HTTP'")
                .doesNotContain("PERSONAL_ALIPAY_QR', 'WEB', 'PROD', 1, 1, 1, 1, 1")
                .doesNotContain("PERSONAL_WECHAT_QR', 'WEB', 'PROD', 1, 1, 1, 1, 1")
                .doesNotContain("\"name\":\"interfaceMode\"")
                .doesNotContain("\"name\":\"termId\"")
                .doesNotContain("\"name\":\"termIp\"")
                .doesNotContain("富友接口终端号")
                .doesNotContain("'privateKey',")
                .doesNotContain("\"name\":\"mchntKey\"")
                .doesNotContain("\"name\":\"merchantCertFileId\"")
                .doesNotContain("\"name\":\"fuiouPublicKeyFileId\"")
                .doesNotContain("\"name\":\"wechatSubAppId\"")
                .doesNotContain("\"name\":\"alipayAppId\"")
                .doesNotContain("\"name\":\"ebankProductCode\"")
                .doesNotContain("\"name\":\"supportedBankCodes\"")
                .doesNotContain("PERSONAL_ALIPAY_PC")
                .doesNotContain("PERSONAL_EBANK_REDIRECT")
                .doesNotContain("CORPORATE_EBANK_REDIRECT")
                .doesNotContain("CORPORATE_OFFLINE_ACCOUNT")
                .doesNotContain("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJc");
    }

    private void assertFuiouSeedDoesNotDeclareBillCapability(String sql) {
        assertThat(sql)
                .contains("'FUIOU_PAY'")
                .contains("富友账单获取协议未完成适配前不声明通道账单能力")
                .contains("NULL, 1, 1, NULL, NOW(), NULL, NOW(), 0)")
                .contains("PERSONAL_ALIPAY_QR', 'WEB', 'PROD', 1, 1, 0, 0, 0")
                .doesNotContain("'MANUAL,FTP,FTPS,HTTP'")
                .doesNotContain("'HTTP', 1, 1")
                .doesNotContain("PERSONAL_ALIPAY_QR', 'WEB', 'PROD', 1, 1, 0, 1, 1");
    }

    private void assertFuiouDisplayConfiguration(String sql) {
        assertThat(sql)
                .contains("'FUIOU_PAY'")
                .contains("\"name\":\"privateKey\"")
                .contains("\"label\":\"商户私钥\"")
                .contains("\"name\":\"fuiouPublicKey\"")
                .contains("\"label\":\"富友平台公钥\"")
                .contains("\"name\":\"gatewayBaseUrl\"")
                .contains("富友接口内部参数由系统按线上收款接口规则处理")
                .doesNotContain("\"name\":\"interfaceMode\"")
                .doesNotContain("\"name\":\"termId\"")
                .doesNotContain("\"name\":\"termIp\"")
                .doesNotContain("XML/RSA")
                .doesNotContain("RSA 密钥")
                .doesNotContain("富友接口终端号");
    }

    private void assertFuiouGatewayConfiguration(String sql) {
        assertThat(sql)
                .contains("'FUIOU_PAY'")
                .contains("\"name\":\"scanpayGatewayBaseUrl\"")
                .contains("\"label\":\"扫码接口地址\"")
                .contains("\"name\":\"gatewayMerchantNo\"")
                .contains("\"label\":\"网关商户号\"")
                .contains("\"name\":\"gatewayMerchantKey\"")
                .contains("\"label\":\"网关商户密钥\"")
                .contains("\"name\":\"gatewayPayUrl\"")
                .contains("\"name\":\"gatewayQueryUrl\"")
                .contains("\"name\":\"gatewayPageNotifyUrl\"")
                .contains("\"name\":\"gatewayBackNotifyUrl\"")
                .contains("PERSONAL_WECHAT_QR")
                .contains("PERSONAL_ALIPAY_QR")
                .contains("PERSONAL_EBANK_REDIRECT")
                .contains("CORPORATE_EBANK_REDIRECT")
                .contains("'gatewayMerchantKey', 'vau6p7ldawpezyaugc0kopdrrwm4gkpu'")
                .contains("http://www-2.wg.fuiou.com:13195/smpGate.do")
                .contains("http://www-2.wg.fuiou.com:13195/smpAQueryGate.do")
                .contains("网银退款和富友账单接口资料未确认前不开放对应能力")
                .contains("`bill_fetch_modes` = NULL")
                .contains("'https://payment.example.com/api/payment/channel-callbacks/fuiou'")
                .contains("'gatewayPageNotifyUrl', 'https://payment.example.com/payment/fuiou/return'")
                .contains("'gatewayBackNotifyUrl', 'https://payment.example.com/api/payment/channel-callbacks/fuiou'")
                .doesNotContain("27.185.20.146")
                .doesNotContain("douxy.inner.yunxinbaokeji.com")
                .doesNotContain("supports_bill`, `supports_reconcile`, `min_amount`, `max_amount`, `status`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`)\nVALUES\n  (332016, 330005, 'PERSONAL_ALIPAY_QR', 'WEB', 'PROD', 1, 1, 1, 1, 1")
                .doesNotContain("\"name\":\"xmlRsa\"")
                .doesNotContain("\"name\":\"interfaceMode\"")
                .doesNotContain("\"name\":\"termId\"")
                .doesNotContain("\"name\":\"termIp\"")
                .doesNotContain("XML/RSA")
                .doesNotContain("RSA 密钥")
                .doesNotContain("签名算法")
                .doesNotContain("报文格式")
                .doesNotContain("SM2");
    }

    private void assertFuiouScanpayPublicKeyFix(String sql) {
        assertThat(sql)
                .contains("'FUIOU_PAY'")
                .contains("'FUIOU_PAY_MANGO_TECH'")
                .contains("'fuiouPublicKey', 'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBv9K+jiuHqXIehX81oyNSD2RfVn+KTPb7NRT5HDPFE35CjZJd7Fu40r0U2Cp7Eyhayv/mRS6ZqvBT/8tQqwpUExTQQBbdZjfk+efb9bF9a+uCnAg0RsuqxeJ2r/rRTsORzVLJy+4GKcv06/p6CcBc5BI1gqSKmyyNBlgfkxLYewIDAQAB'");
    }
}
