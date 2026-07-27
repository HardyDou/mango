package io.mango.app.platformcapability.calendar;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Calendar 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoCalendarCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoCalendarCapabilityApplication.class, args);
    }
}
