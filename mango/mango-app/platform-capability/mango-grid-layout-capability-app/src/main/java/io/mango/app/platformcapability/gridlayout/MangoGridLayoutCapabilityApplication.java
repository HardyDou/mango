package io.mango.app.platformcapability.gridlayout;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango GridLayout 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoGridLayoutCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoGridLayoutCapabilityApplication.class, args);
    }
}
