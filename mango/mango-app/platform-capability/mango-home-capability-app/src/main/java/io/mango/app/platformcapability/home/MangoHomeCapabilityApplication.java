package io.mango.app.platformcapability.home;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Home 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoHomeCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoHomeCapabilityApplication.class, args);
    }
}
