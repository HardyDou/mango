package io.mango.file.preview.starter;

import cn.keking.config.ConfigConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewTrustHostInitializerTest {

    @AfterEach
    void resetTrustedHosts() {
        ConfigConstants.setTrustHostValue("default");
    }

    @Test
    void run_addsSourceBaseHostWithoutLosingConfiguredHosts() {
        ConfigConstants.setTrustHostValue("localhost,preview.mango.test");
        new FilePreviewTrustHostInitializer("http://127.0.0.1:18018/internal/").run(null);

        assertThat(ConfigConstants.getTrustHostSet())
                .contains("localhost", "preview.mango.test", "127.0.0.1");
    }

    @Test
    void run_whenSourceBaseUrlMissing_keepsConfiguredHosts() {
        ConfigConstants.setTrustHostValue("localhost");
        new FilePreviewTrustHostInitializer(null).run(null);

        assertThat(ConfigConstants.getTrustHostSet()).containsExactly("localhost");
    }

    @Test
    void run_whenSourceBaseUrlInvalid_keepsConfiguredHosts() {
        ConfigConstants.setTrustHostValue("localhost");
        new FilePreviewTrustHostInitializer("not a valid url").run(null);

        assertThat(ConfigConstants.getTrustHostSet()).containsExactly("localhost");
    }
}
