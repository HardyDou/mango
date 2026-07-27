package io.mango.app.platformcapability.file;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango File 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoFileCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoFileCapabilityApplication.class, args);
    }
}
