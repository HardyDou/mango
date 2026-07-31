package io.mango.app.platformcapability.payment;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Payment 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoPaymentCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoPaymentCapabilityApplication.class, args);
    }
}
