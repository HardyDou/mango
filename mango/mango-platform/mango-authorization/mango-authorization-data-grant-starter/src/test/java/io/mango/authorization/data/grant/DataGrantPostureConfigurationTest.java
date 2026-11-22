package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Grant 保护姿态日志测试。
 *
 * <p>三种姿态必须各自可辨识：关闭、缺密钥、完整启用。</p>
 */
@ExtendWith(OutputCaptureExtension.class)
class DataGrantPostureConfigurationTest {

    @Test
    void disabledPostureIsLoggedAsWarning(CapturedOutput output) {
        DataGrantProperties properties = new DataGrantProperties();
        properties.setEnabled(false);

        new DataGrantPostureConfiguration(properties);

        assertThat(output).contains("数据权限 Grant 已关闭");
    }

    @Test
    void missingSecretPostureIsLoggedAsWarning(CapturedOutput output) {
        DataGrantProperties properties = new DataGrantProperties();
        properties.setSecret(null);

        new DataGrantPostureConfiguration(properties);

        assertThat(output).contains("未配置 mango.data-grant.secret");
    }

    @Test
    void enabledPostureIsLoggedWithRuntimeSettings(CapturedOutput output) {
        DataGrantProperties properties = GrantTestSupport.properties();

        new DataGrantPostureConfiguration(properties);

        assertThat(output)
                .contains("数据权限 Grant 已启用")
                .contains("maxIds=" + properties.getMaxIds());
    }
}
