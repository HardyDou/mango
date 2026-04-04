package io.mango.ai.starter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * AI service auto configuration
 *
 * @author Mango
 */
@Configuration
@ComponentScan({
        "io.mango.ai.core.controller",
        "io.mango.ai.core.service",
        "io.mango.ai.core.provider"
})
public class MangoAiAutoConfiguration {
}
