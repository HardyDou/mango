package io.mango.infra.sse.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * SSE auto configuration
 *
 * @author Mango
 */
@Configuration
@ComponentScan({
        "io.mango.infra.sse.controller",
        "io.mango.infra.sse.service"
})
public class MangoSseAutoConfiguration {
}
