package io.mango.file.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilePropertiesTest {

    @Test
    void bindsExternalAssetRootFromKebabCaseConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(
                Map.of("mango.file.asset-root", "/opt/mango/bootstrap-assets"));

        FileProperties properties = new Binder(source)
                .bind("mango.file", Bindable.of(FileProperties.class))
                .get();

        assertThat(properties.getAssetRoot()).isEqualTo("/opt/mango/bootstrap-assets");
    }
}
