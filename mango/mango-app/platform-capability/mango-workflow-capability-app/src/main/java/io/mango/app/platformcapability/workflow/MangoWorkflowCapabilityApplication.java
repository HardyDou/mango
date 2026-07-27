package io.mango.app.platformcapability.workflow;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Workflow 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoWorkflowCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoWorkflowCapabilityApplication.class, args);
    }
}
