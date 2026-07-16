package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.common.exception.BizException;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.mango.system.api.vo.SysConfigVO;
import io.mango.system.core.entity.SysConfigEntity;
import io.mango.system.core.mapper.SysConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper sysConfigMapper;

    private SysConfigService sysConfigService;

    @BeforeEach
    void setUp() {
        sysConfigService = new SysConfigService(sysConfigMapper);
    }

    @Test
    void listDefaultsMissingPanelMetadataAndAcceptsCaseInsensitiveType() {
        SysConfigEntity config = config();
        config.setValueType(null);
        config.setOptionSource(null);
        config.setEditable(null);
        when(sysConfigMapper.selectList(any(Wrapper.class))).thenReturn(List.of(config));

        List<SysConfigVO> result = sysConfigService.list(" system ", "COMMON");

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getValueType()).isEqualTo(ConfigValueTypeEnum.STRING);
            assertThat(item.getOptionSource()).isEqualTo(ConfigOptionSourceEnum.CUSTOM);
            assertThat(item.getEditable()).isTrue();
        });
    }

    @Test
    void listRejectsInvalidTypeWithModuleCode() {
        assertThatThrownBy(() -> sysConfigService.list("unknown", "COMMON"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("配置类型不合法");
    }

    @Test
    void updateValueRejectsDisabledConfig() {
        SysConfigEntity config = config();
        config.setStatus(0);
        when(sysConfigMapper.selectById(1L)).thenReturn(config);

        assertThatThrownBy(() -> sysConfigService.updateValue(1L, "new-value"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("配置已禁用");
        verify(sysConfigMapper, never()).updateById(any(SysConfigEntity.class));
    }

    @Test
    void updateValueRejectsReadonlyConfig() {
        SysConfigEntity config = config();
        config.setEditable(false);
        config.setEditableReason("由系统托管");
        when(sysConfigMapper.selectById(1L)).thenReturn(config);

        assertThatThrownBy(() -> sysConfigService.updateValue(1L, "new-value"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("由系统托管");
    }

    @Test
    void updateValueUpdatesEditableConfig() {
        when(sysConfigMapper.selectById(1L)).thenReturn(config());
        when(sysConfigMapper.updateById(any(SysConfigEntity.class))).thenReturn(1);

        assertThat(sysConfigService.updateValue(1L, "new-value")).isTrue();

        ArgumentCaptor<SysConfigEntity> captor = ArgumentCaptor.forClass(SysConfigEntity.class);
        verify(sysConfigMapper).updateById(captor.capture());
        assertThat(captor.getValue().getConfigValue()).isEqualTo("new-value");
    }

    @Test
    void typedProvidersKeepConfiguredAndDefaultValues() {
        SysConfigEntity config = config();
        config.setConfigValue("true");
        when(sysConfigMapper.selectOne(any(Wrapper.class))).thenReturn(config, null);

        assertThat(sysConfigService.getBooleanValue("feature.enabled", false)).isTrue();
        assertThat(sysConfigService.getIntegerValue("max.retry", 3)).isEqualTo(3);
    }

    private SysConfigEntity config() {
        SysConfigEntity config = new SysConfigEntity();
        config.setId(1L);
        config.setConfigKey("feature.enabled");
        config.setConfigName("功能开关");
        config.setConfigValue("false");
        config.setDomainCode("COMMON");
        config.setStatus(1);
        config.setEditable(true);
        return config;
    }
}
