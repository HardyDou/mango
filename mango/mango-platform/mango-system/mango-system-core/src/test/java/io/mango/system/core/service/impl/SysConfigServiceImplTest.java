package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.common.result.R;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.mango.system.api.po.SysConfigPo;
import io.mango.system.core.entity.SysConfig;
import io.mango.system.core.mapper.SysConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysConfigServiceImpl Tests")
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper sysConfigMapper;

    private SysConfigServiceImpl sysConfigService;

    @BeforeEach
    void setUp() {
        sysConfigService = new SysConfigServiceImpl(sysConfigMapper);
    }

    @Test
    @DisplayName("list should default missing panel metadata")
    void list_missingPanelMetadata_returnsCompatibleDefaults() {
        SysConfig config = createConfig();
        config.setValueType(null);
        config.setEditable(null);
        when(sysConfigMapper.selectList(any(Wrapper.class))).thenReturn(List.of(config));

        R<List<SysConfigPo>> result = sysConfigService.list(null, "COMMON");

        assertTrue(result.isSuccess());
        assertEquals(ConfigValueTypeEnum.STRING, result.getData().get(0).getValueType());
        assertEquals(ConfigOptionSourceEnum.CUSTOM, result.getData().get(0).getOptionSource());
        assertTrue(result.getData().get(0).getEditable());
    }

    @Test
    @DisplayName("updateValue should reject disabled config")
    void updateValue_disabledConfig_returnsFail() {
        SysConfig config = createConfig();
        config.setStatus(0);
        when(sysConfigMapper.selectById(1L)).thenReturn(config);

        R<Boolean> result = sysConfigService.updateValue(1L, "new-value");

        assertFalse(result.isSuccess());
        assertEquals("配置已禁用", result.getMsg());
        verify(sysConfigMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    @DisplayName("updateValue should reject readonly config")
    void updateValue_readonlyConfig_returnsFail() {
        SysConfig config = createConfig();
        config.setEditable(false);
        config.setEditableReason("由系统托管");
        when(sysConfigMapper.selectById(1L)).thenReturn(config);

        R<Boolean> result = sysConfigService.updateValue(1L, "new-value");

        assertFalse(result.isSuccess());
        assertEquals("由系统托管", result.getMsg());
        verify(sysConfigMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    @DisplayName("updateValue should update editable config")
    void updateValue_editableConfig_updatesValue() {
        SysConfig config = createConfig();
        when(sysConfigMapper.selectById(1L)).thenReturn(config);
        when(sysConfigMapper.updateById(any(SysConfig.class))).thenReturn(1);

        R<Boolean> result = sysConfigService.updateValue(1L, "new-value");

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(sysConfigMapper).updateById(captor.capture());
        assertTrue(result.isSuccess());
        assertEquals("new-value", captor.getValue().getConfigValue());
    }

    @Test
    @DisplayName("getBooleanValue should return configured boolean")
    void getBooleanValue_existingConfig_returnsBoolean() {
        SysConfig config = createConfig();
        config.setConfigValue("true");
        when(sysConfigMapper.selectOne(any(Wrapper.class))).thenReturn(config);

        R<Boolean> result = sysConfigService.getBooleanValue("feature.enabled", false);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
    }

    @Test
    @DisplayName("getIntegerValue should return default when missing")
    void getIntegerValue_missingConfig_returnsDefault() {
        when(sysConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        R<Integer> result = sysConfigService.getIntegerValue("max.retry", 3);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getData());
    }

    private SysConfig createConfig() {
        SysConfig config = new SysConfig();
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
