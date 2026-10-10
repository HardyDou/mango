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
        String[] requestedArgs = args == null ? new String[0] : args.clone();
        if (requestedArgs.length == 0 || requestedArgs[0].trim().startsWith("--")) {
            return runLocal(primarySource, requestedArgs);
        }
        String mode = requestedArgs[0].trim().toLowerCase(Locale.ROOT);
        if (!"bootstrap".equals(mode) && !"runtime".equals(mode)) {
            throw new IllegalArgumentException("Unsupported Mango process mode: " + requestedArgs[0]);
        }
        return runExplicit(primarySource, requestedArgs, mode);
    }

    private static ConfigurableApplicationContext runExplicit(
            Class<?> primarySource, String[] args, String mode) {
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
        addLifecycleInitializer(application, defaults, Map.of(), null);
        String[] springArgs = Arrays.copyOfRange(args, consumed, args.length);
        ConfigurableApplicationContext context = application.run(springArgs);
        if ("bootstrap".equals(mode)) {
            context.close();
        }
        return context;
    }

    private static ConfigurableApplicationContext runLocal(Class<?> primarySource, String[] requestedArgs) {
        Map<String, Object> workspaceProperties = MangoLocalWorkspace.loadProperties();
        String[] localArgs = append(MangoLocalWorkspace.additionalArguments(workspaceProperties), requestedArgs);
        MangoLocalWorkspace.validateDatabase(workspaceProperties, localArgs);
        MangoLocalWorkspace.prepareDatabase(workspaceProperties);
        MangoLocalStartupState startupState = new MangoLocalStartupState();

        SpringApplication bootstrap = new SpringApplication(primarySource);
        bootstrap.setWebApplicationType(WebApplicationType.NONE);
        bootstrap.setLazyInitialization(true);
        Map<String, Object> bootstrapDefaults = new LinkedHashMap<>();
        bootstrapDefaults.put("mango.bootstrap.mode", "bootstrap");
        bootstrapDefaults.put("mango.bootstrap.action", "apply");
        bootstrapDefaults.put("mango.bootstrap.strategy", "cold");
        bootstrapDefaults.put("mango.bootstrap.local-startup", "true");
        bootstrapDefaults.put("spring.flyway.enabled", "false");
        bootstrapDefaults.put("spring.task.scheduling.enabled", "false");
        addLifecycleInitializer(bootstrap, bootstrapDefaults, workspaceProperties, startupState);

        ConfigurableApplicationContext bootstrapContext = bootstrap.run(localArgs);
        MangoLocalRuntimeIdentity identity;
        try {
            identity = startupState.requireIdentity();
        } finally {
            bootstrapContext.close();
        }

        SpringApplication runtime = new SpringApplication(primarySource);
        Map<String, Object> runtimeDefaults = new LinkedHashMap<>();
        runtimeDefaults.put("mango.bootstrap.mode", "runtime");
        runtimeDefaults.put("mango.bootstrap.environment-key", identity.environmentKey());
        runtimeDefaults.put("mango.release.id", identity.releaseId());
        runtimeDefaults.put("mango.release.revision", identity.revision());
        runtimeDefaults.put("mango.release.generation", identity.generation());
        runtimeDefaults.put("mango.release.fingerprint", identity.fingerprint());
        runtimeDefaults.put("spring.flyway.enabled", "false");
        addLifecycleInitializer(runtime, runtimeDefaults, workspaceProperties, null);
        return runtime.run(localArgs);
    }

    private static void addLifecycleInitializer(SpringApplication application,
                                                Map<String, Object> lifecycleProperties,
                                                Map<String, Object> workspaceProperties,
                                                MangoLocalStartupState startupState) {
        Map<String, Object> immutableLifecycleProperties = Map.copyOf(lifecycleProperties);
        application.addInitializers(context -> {
            MangoLocalWorkspace.addPropertySource(context.getEnvironment(), workspaceProperties);
            if (startupState != null) {
                context.getBeanFactory().registerSingleton("mangoLocalStartupState", startupState);
            }
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("mangoLifecycleCommand", immutableLifecycleProperties));
        });
    }

    private static String[] append(String[] prefix, String[] suffix) {
        String[] result = Arrays.copyOf(prefix, prefix.length + suffix.length);
        System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
        return result;
    }
}
