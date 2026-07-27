package io.mango.app.platformcapability.auth;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Auth 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoAuthCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoAuthCapabilityApplication.class, args);
    }
}
