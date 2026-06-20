package io.mango.app.platformcapability.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Authorization 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoAuthorizationCapabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoAuthorizationCapabilityApplication.class, args);
    }
}
