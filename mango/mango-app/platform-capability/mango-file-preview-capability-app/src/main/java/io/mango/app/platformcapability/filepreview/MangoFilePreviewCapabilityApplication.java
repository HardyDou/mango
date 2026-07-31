package io.mango.app.platformcapability.filepreview;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango FilePreview 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoFilePreviewCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoFilePreviewCapabilityApplication.class, args);
    }
}
