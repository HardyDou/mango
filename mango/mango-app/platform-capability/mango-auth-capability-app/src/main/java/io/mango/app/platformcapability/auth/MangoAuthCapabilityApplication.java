package io.mango.app.platformcapability.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Auth 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoAuthCapabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoAuthCapabilityApplication.class, args);
    }
}
