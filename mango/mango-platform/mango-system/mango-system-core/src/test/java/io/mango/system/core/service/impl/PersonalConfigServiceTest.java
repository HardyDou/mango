package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.system.api.command.SavePersonalConfigCommand;
import io.mango.system.api.query.PersonalConfigQuery;
import io.mango.system.api.vo.PersonalConfigVO;
import io.mango.system.core.entity.SysPersonalConfigEntity;
import io.mango.system.core.mapper.SysPersonalConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalConfigService Tests")
class PersonalConfigServiceTest {

    @Mock
    private SysPersonalConfigMapper personalConfigMapper;

    private PersonalConfigService personalConfigService;

    @BeforeEach
    void setUp() {
        personalConfigService = new PersonalConfigService(personalConfigMapper);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("saveCurrentUser should insert config for current tenant and user")
    void saveCurrentUser_newConfig_insertsCurrentUserConfig() {
        when(personalConfigMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(personalConfigMapper.insert(any(SysPersonalConfigEntity.class))).thenReturn(1);

        PersonalConfigVO result = personalConfigService.saveCurrentUser(createCommand());

        ArgumentCaptor<SysPersonalConfigEntity> captor = ArgumentCaptor.forClass(SysPersonalConfigEntity.class);
        verify(personalConfigMapper).insert(captor.capture());
        SysPersonalConfigEntity entity = captor.getValue();
        assertEquals("1", entity.getTenantId());
        assertEquals(1001L, entity.getUserId());
        assertEquals("notice", entity.getGroupCode());
        assertEquals("client_reminder", entity.getBizType());
        assertEquals("reminder_setting", entity.getConfigKey());
        assertEquals("JSON", entity.getValueType());
        assertEquals("{\"popupEnabled\":true}", result.getConfigValue());
    }

    @Test
    @DisplayName("saveCurrentUser should update existing config")
    void saveCurrentUser_existingConfig_updatesValue() {
        SysPersonalConfigEntity existing = new SysPersonalConfigEntity();
        existing.setId(10L);
        existing.setTenantId("1");
        existing.setUserId(1001L);
        existing.setGroupCode("notice");
        existing.setBizType("client_reminder");
        existing.setConfigKey("reminder_setting");
        when(personalConfigMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(personalConfigMapper.updateById(any(SysPersonalConfigEntity.class))).thenReturn(1);

        PersonalConfigVO result = personalConfigService.saveCurrentUser(createCommand());

        verify(personalConfigMapper).updateById(existing);
        verify(personalConfigMapper, never()).insert(any(SysPersonalConfigEntity.class));
        assertEquals(10L, result.getId());
        assertEquals("{\"popupEnabled\":true}", existing.getConfigValue());
    }

    @Test
    @DisplayName("getCurrentUserValue should require full key")
    void getCurrentUserValue_missingKey_throwsBizException() {
        PersonalConfigQuery query = new PersonalConfigQuery();
        query.setGroupCode("notice");
        query.setBizType("client_reminder");

        assertThrows(BizException.class, () -> personalConfigService.getCurrentUserValue(query));
    }

    @Test
    @DisplayName("listCurrentUser should return current user's configs")
    void listCurrentUser_returnsConfigs() {
        SysPersonalConfigEntity entity = new SysPersonalConfigEntity();
        entity.setId(1L);
        entity.setTenantId("1");
        entity.setUserId(1001L);
        entity.setGroupCode("notice");
        entity.setBizType("client_reminder");
        entity.setConfigKey("reminder_setting");
        entity.setConfigValue("{}");
        when(personalConfigMapper.selectList(any(Wrapper.class))).thenReturn(List.of(entity));

        List<PersonalConfigVO> result = personalConfigService.listCurrentUser(new PersonalConfigQuery());

        assertEquals(1, result.size());
        assertEquals("notice", result.get(0).getGroupCode());
        assertNotNull(result.get(0));
    }

    @Test
    @DisplayName("deleteCurrentUser should delete current user's exact config")
    void deleteCurrentUser_deletesConfig() {
        when(personalConfigMapper.delete(any(Wrapper.class))).thenReturn(1);
        PersonalConfigQuery query = new PersonalConfigQuery();
        query.setGroupCode("notice");
        query.setBizType("client_reminder");
        query.setConfigKey("reminder_setting");

        boolean result = personalConfigService.deleteCurrentUser(query);

        assertTrue(result);
        verify(personalConfigMapper).delete(any(Wrapper.class));
    }

    private SavePersonalConfigCommand createCommand() {
        SavePersonalConfigCommand command = new SavePersonalConfigCommand();
        command.setGroupCode("notice");
        command.setBizType("client_reminder");
        command.setConfigKey("reminder_setting");
        command.setConfigValue("{\"popupEnabled\":true}");
        command.setConfigName("通知提醒设置");
        return command;
    }
}
