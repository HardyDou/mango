package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.common.exception.BizException;
import io.mango.system.api.command.SaveAdminBrandingCommand;
import io.mango.system.api.vo.AdminBrandingVO;
import io.mango.system.core.entity.SysConfigEntity;
import io.mango.system.core.mapper.SysConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AdminBrandingService Tests")
class AdminBrandingServiceTest {

    private final SysConfigMapper sysConfigMapper = mock(SysConfigMapper.class);

    private AdminBrandingService adminBrandingService;

    @BeforeEach
    void setUp() {
        adminBrandingService = new AdminBrandingService(sysConfigMapper);
    }

    @Test
    @DisplayName("get should return defaults when config is missing")
    void get_missingConfig_returnsDefaults() {
        when(sysConfigMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        AdminBrandingVO result = adminBrandingService.get();

        assertEquals("Mango Admin", result.getTitle());
        assertTrue(result.getEnabled());
        assertEquals("Mango", result.getShortTitle());
        assertEquals("企业级管理平台", result.getSubtitle());
        assertEquals("", result.getLogoIconFile());
        assertEquals("© Mango", result.getFooterCopyright());
    }

    @Test
    @DisplayName("get should preserve blank text values when config exists")
    void get_blankTextConfig_preservesBlankValues() {
        when(sysConfigMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                config("admin.branding.title", ""),
                config("admin.branding.shortTitle", ""),
                config("admin.branding.loginTitle", ""),
                config("admin.branding.loginSubtitle", ""),
                config("admin.branding.footerCopyright", "")
        ));

        AdminBrandingVO result = adminBrandingService.get();

        assertEquals("", result.getTitle());
        assertEquals("", result.getShortTitle());
        assertEquals("企业级管理平台", result.getSubtitle());
        assertEquals("", result.getLoginTitle());
        assertEquals("", result.getLoginSubtitle());
        assertEquals("", result.getFooterCopyright());
    }

    @Test
    @DisplayName("save should insert normalized file id")
    void save_newConfig_insertsFileId() {
        when(sysConfigMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(sysConfigMapper.insert(any(SysConfigEntity.class))).thenReturn(1);

        SaveAdminBrandingCommand command = createCommand();
        command.setLogoFile(" 1888888888888888888 ");
        command.setLogoIconFile(" 1888888888888888889 ");

        assertTrue(adminBrandingService.save(command));

        ArgumentCaptor<SysConfigEntity> captor = ArgumentCaptor.forClass(SysConfigEntity.class);
        verify(sysConfigMapper, times(13)).insert(captor.capture());
        SysConfigEntity firstInserted = captor.getAllValues().get(0);
        assertEquals("admin.branding.enabled", firstInserted.getConfigKey());
        assertEquals("true", firstInserted.getConfigValue());
        assertEquals("网站配置", firstInserted.getGroupName());
        assertTrue(firstInserted.getEditable());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(config -> "admin.branding.logoFile".equals(config.getConfigKey())
                        && "1888888888888888888".equals(config.getConfigValue())));
        assertTrue(captor.getAllValues().stream()
                .anyMatch(config -> "admin.branding.logoIconFile".equals(config.getConfigKey())
                        && "1888888888888888889".equals(config.getConfigValue())
                        && Integer.valueOf(65).equals(config.getSort())));
    }

    @Test
    @DisplayName("save should normalize legacy file token to file id")
    void save_legacyFileToken_normalizesToFileId() {
        when(sysConfigMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(sysConfigMapper.insert(any(SysConfigEntity.class))).thenReturn(1);

        SaveAdminBrandingCommand command = createCommand();
        command.setLogoIconFile(" mango-file:1888888888888888888 ");

        assertTrue(adminBrandingService.save(command));

        ArgumentCaptor<SysConfigEntity> captor = ArgumentCaptor.forClass(SysConfigEntity.class);
        verify(sysConfigMapper, times(13)).insert(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(config -> "admin.branding.logoIconFile".equals(config.getConfigKey())
                        && "1888888888888888888".equals(config.getConfigValue())));
    }

    @Test
    @DisplayName("save should update existing config")
    void save_existingConfig_updatesValue() {
        SysConfigEntity existing = new SysConfigEntity();
        existing.setId(1L);
        existing.setConfigKey("admin.branding.title");
        existing.setConfigValue("旧后台");
        when(sysConfigMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));
        when(sysConfigMapper.insert(any(SysConfigEntity.class))).thenReturn(1);
        when(sysConfigMapper.updateById(any(SysConfigEntity.class))).thenReturn(1);

        adminBrandingService.save(createCommand());

        ArgumentCaptor<SysConfigEntity> captor = ArgumentCaptor.forClass(SysConfigEntity.class);
        verify(sysConfigMapper).updateById(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals("新后台", captor.getValue().getConfigValue());
    }

    @Test
    @DisplayName("save should reject preview url for file fields")
    void save_filePreviewUrl_throwsBizException() {
        SaveAdminBrandingCommand command = createCommand();
        command.setLogoFile("https://example.com/logo.png");

        assertThrows(BizException.class, () -> adminBrandingService.save(command));
        verify(sysConfigMapper, never()).insert(any(SysConfigEntity.class));
        verify(sysConfigMapper, never()).updateById(any(SysConfigEntity.class));
    }

    private SysConfigEntity config(String key, String value) {
        SysConfigEntity config = new SysConfigEntity();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setStatus(1);
        return config;
    }

    private SaveAdminBrandingCommand createCommand() {
        SaveAdminBrandingCommand command = new SaveAdminBrandingCommand();
        command.setEnabled(true);
        command.setTitle("新后台");
        command.setShortTitle("新后台");
        command.setSubtitle("新后台副标题");
        command.setLoginTitle("新登录标题");
        command.setLoginSubtitle("新登录副标题");
        command.setLogoFile("");
        command.setLogoIconFile("");
        command.setFaviconFile("");
        command.setLoginImageFile("");
        command.setFooterCopyright("© 新后台");
        command.setIcp("");
        command.setContact("");
        return command;
    }
}
