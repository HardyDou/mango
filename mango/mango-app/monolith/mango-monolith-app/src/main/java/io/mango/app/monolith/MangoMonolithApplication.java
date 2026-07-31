package io.mango.app.monolith;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 单体部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
public class MangoMonolithApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoMonolithApplication.class, args);
    }
}
