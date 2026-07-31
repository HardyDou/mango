package io.mango.app.platformcapability.numgen;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Numgen 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoNumgenCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoNumgenCapabilityApplication.class, args);
    }
}
