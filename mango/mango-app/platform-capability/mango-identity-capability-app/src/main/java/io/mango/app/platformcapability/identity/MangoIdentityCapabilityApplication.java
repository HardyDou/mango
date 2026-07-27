package io.mango.app.platformcapability.identity;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Identity 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoIdentityCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoIdentityCapabilityApplication.class, args);
    }
}
