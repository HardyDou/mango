package io.mango.app.platformcapability.resource;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Resource 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoResourceCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoResourceCapabilityApplication.class, args);
    }
}
