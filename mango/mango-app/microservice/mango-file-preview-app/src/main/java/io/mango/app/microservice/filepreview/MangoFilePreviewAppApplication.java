package io.mango.app.microservice.filepreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 文件预览独立部署入口。
 */
@SpringBootApplication
public class MangoFilePreviewAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoFilePreviewAppApplication.class, args);
    }
}
