package io.mango.app.platformcapability.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Payment 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoPaymentCapabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoPaymentCapabilityApplication.class, args);
    }
}
