package io.mango.app.microservice.filepreview;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 文件预览独立部署入口。
 */
@SpringBootApplication
public class MangoFilePreviewAppApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoFilePreviewAppApplication.class, args);
    }
}
