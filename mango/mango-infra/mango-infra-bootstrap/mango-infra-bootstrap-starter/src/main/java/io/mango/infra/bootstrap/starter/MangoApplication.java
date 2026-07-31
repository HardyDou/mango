package io.mango.infra.bootstrap.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MangoApplication {

    private MangoApplication() {
    }

    public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Mango process mode is required: bootstrap or runtime");
        }
        String mode = args[0].trim().toLowerCase(Locale.ROOT);
        if (!"bootstrap".equals(mode) && !"runtime".equals(mode)) {
            throw new IllegalArgumentException("Unsupported Mango process mode: " + args[0]);
        }
        SpringApplication application = new SpringApplication(primarySource);
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("mango.bootstrap.mode", mode);
        defaults.put("spring.flyway.enabled", "false");
        int consumed = 1;
        if ("bootstrap".equals(mode)) {
            if (args.length < 2 || args[1].startsWith("--")) {
                throw new IllegalArgumentException(
                        "Mango bootstrap action is required: plan, apply, verify, finalize or abort");
            }
            defaults.put("mango.bootstrap.action", args[1].trim().toLowerCase(Locale.ROOT));
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setLazyInitialization(true);
            defaults.put("spring.task.scheduling.enabled", "false");
            consumed = 2;
        }
        Map<String, Object> lifecycleProperties = Map.copyOf(defaults);
        application.addInitializers(context -> context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("mangoLifecycleCommand", lifecycleProperties)));
        String[] springArgs = Arrays.copyOfRange(args, consumed, args.length);
        ConfigurableApplicationContext context = application.run(springArgs);
        if ("bootstrap".equals(mode)) {
            context.close();
        }
        return context;
    }
}
