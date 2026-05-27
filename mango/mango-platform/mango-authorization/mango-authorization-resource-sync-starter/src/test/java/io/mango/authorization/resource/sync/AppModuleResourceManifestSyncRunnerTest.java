package io.mango.authorization.resource.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AppModuleApi;
import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.api.vo.FrontendModuleRuntimeStrategyVO;
import io.mango.common.result.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("App module resource manifest sync runner tests")
class AppModuleResourceManifestSyncRunnerTest {

    @Test
    @DisplayName("loadManifests should read classpath resource manifest")
    void loadManifests_classpathResource_returnsManifest() {
        AppModuleResourceManifestSyncProperties properties = new AppModuleResourceManifestSyncProperties();
        TestAppModuleApi appModuleApi = new TestAppModuleApi();
        AppModuleResourceManifestSyncRunner runner =
                new AppModuleResourceManifestSyncRunner(appModuleApi, new ObjectMapper(), properties);

        List<AppModuleResourceManifestCommand> manifests = runner.loadManifests();

        assertEquals(1, manifests.size());
        assertEquals("internal-admin", manifests.get(0).getAppCode());
        assertEquals("guarantee", manifests.get(0).getModuleCode());
        assertEquals(1, manifests.get(0).getMenus().size());
    }

    @Test
    @DisplayName("run should register manifests in write mode")
    void run_writeMode_registersManifest() {
        AppModuleResourceManifestSyncProperties properties = new AppModuleResourceManifestSyncProperties();
        TestAppModuleApi appModuleApi = new TestAppModuleApi();
        AppModuleResourceManifestSyncRunner runner =
                new AppModuleResourceManifestSyncRunner(appModuleApi, new ObjectMapper(), properties);

        runner.run(null);

        assertEquals(1, appModuleApi.manifests.size());
        assertEquals("guarantee", appModuleApi.manifests.get(0).getModuleCode());
    }

    @Test
    @DisplayName("run should not register manifests in read mode")
    void run_readMode_doesNotRegisterManifest() {
        AppModuleResourceManifestSyncProperties properties = new AppModuleResourceManifestSyncProperties();
        properties.setMode("read");
        TestAppModuleApi appModuleApi = new TestAppModuleApi();
        AppModuleResourceManifestSyncRunner runner =
                new AppModuleResourceManifestSyncRunner(appModuleApi, new ObjectMapper(), properties);

        runner.run(null);

        assertEquals(0, appModuleApi.manifests.size());
    }

    private static class TestAppModuleApi implements AppModuleApi {

        private final List<AppModuleResourceManifestCommand> manifests = new ArrayList<>();

        @Override
        public R<List<AppModuleVO>> list(String appCode, Integer status) {
            return R.ok(List.of());
        }

        @Override
        public R<Long> save(AppModuleCommand command) {
            return R.ok(1L);
        }

        @Override
        public R<Boolean> disable(String appCode, String moduleCode) {
            return R.ok(true);
        }

        @Override
        public R<Integer> syncMenus(String appCode, String moduleCode) {
            return R.ok(0);
        }

        @Override
        public R<Integer> registerResourceManifest(AppModuleResourceManifestCommand command) {
            manifests.add(command);
            return R.ok(1);
        }

        @Override
        public R<List<FrontendModuleRuntimeStrategyVO>> listRuntimeStrategies(String appCode, String deployProfile) {
            return R.ok(List.of());
        }

        @Override
        public R<Long> saveRuntimeStrategy(FrontendModuleRuntimeStrategyCommand command) {
            return R.ok(1L);
        }
    }
}
