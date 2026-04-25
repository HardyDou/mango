package io.mango.app.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 单体部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
public class MangoMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoMonolithApplication.class, args);
    }
}
