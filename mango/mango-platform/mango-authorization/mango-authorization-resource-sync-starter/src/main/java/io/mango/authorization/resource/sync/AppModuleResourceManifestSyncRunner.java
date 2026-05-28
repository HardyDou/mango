package io.mango.authorization.resource.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AppModuleApi;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 加载 classpath 中的应用模块资源清单并注册到授权服务。
 */
@Slf4j
@RequiredArgsConstructor
public class AppModuleResourceManifestSyncRunner implements ApplicationRunner {

    private final AppModuleApi appModuleApi;
    private final ObjectMapper objectMapper;
    private final AppModuleResourceManifestSyncProperties properties;
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("App module resource manifest sync disabled");
            return;
        }
        List<AppModuleResourceManifestCommand> manifests = loadManifests();
        if (manifests.isEmpty()) {
            log.info("App module resource manifest sync skipped: no manifests discovered");
            return;
        }
        if ("read".equalsIgnoreCase(properties.getMode())) {
            log.info("App module resource manifest sync read-only: discovered {} manifests", manifests.size());
            return;
        }
        int registered = 0;
        for (AppModuleResourceManifestCommand manifest : manifests) {
            R<Integer> response = appModuleApi.registerResourceManifest(manifest);
            if (response != null && response.isSuccess() && response.getData() != null) {
                registered += response.getData();
            } else {
                log.warn("App module resource manifest register failed: appCode={}, moduleCode={}",
                        manifest.getAppCode(), manifest.getModuleCode());
            }
        }
        log.info("App module resource manifest sync complete: manifests={}, registered={}",
                manifests.size(), registered);
    }

    List<AppModuleResourceManifestCommand> loadManifests() {
        List<AppModuleResourceManifestCommand> manifests = new ArrayList<>();
        for (String location : properties.getLocations()) {
            if (!StringUtils.hasText(location)) {
                continue;
            }
            manifests.addAll(loadLocation(location));
        }
        return manifests;
    }

    private List<AppModuleResourceManifestCommand> loadLocation(String location) {
        try {
            Resource[] resources = resourceResolver.getResources(location);
            List<AppModuleResourceManifestCommand> manifests = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) {
                    continue;
                }
                manifests.add(readManifest(resource));
            }
            return manifests;
        } catch (IOException e) {
            throw new IllegalStateException("Load app module resource manifests failed: " + location, e);
        }
    }

    private AppModuleResourceManifestCommand readManifest(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, AppModuleResourceManifestCommand.class);
        }
    }
}
