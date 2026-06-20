package io.mango.gridlayout.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.gridlayout.api.command.SaveGridLayoutPersonalCommand;
import io.mango.gridlayout.api.query.GridLayoutPersonalQuery;
import io.mango.gridlayout.api.vo.GridLayoutPersonalVO;
import io.mango.gridlayout.core.entity.MangoUserGridLayoutEntity;
import io.mango.gridlayout.core.mapper.MangoUserGridLayoutMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GridLayoutPersonalService Tests")
class GridLayoutPersonalServiceTest {

    @Mock
    private MangoUserGridLayoutMapper gridLayoutMapper;

    private GridLayoutPersonalService gridLayoutPersonalService;

    @BeforeEach
    void setUp() {
        gridLayoutPersonalService = new GridLayoutPersonalService(gridLayoutMapper, new ObjectMapper());
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("savePersonal should insert current user layout")
    void savePersonal_newLayout_insertsCurrentUserLayout() {
        when(gridLayoutMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(gridLayoutMapper.insert(any(MangoUserGridLayoutEntity.class))).thenReturn(1);

        GridLayoutPersonalVO result = gridLayoutPersonalService.savePersonal(createCommand());

        ArgumentCaptor<MangoUserGridLayoutEntity> captor = ArgumentCaptor.forClass(MangoUserGridLayoutEntity.class);
        verify(gridLayoutMapper).insert(captor.capture());
        MangoUserGridLayoutEntity entity = captor.getValue();
        assertEquals("1", entity.getTenantId());
        assertEquals(1001L, entity.getUserId());
        assertEquals("admin-home-workbench", entity.getPageCode());
        assertEquals(1, entity.getSchemaVersion());
        assertEquals(entity.getLayoutJson(), result.getLayoutJson());
    }

    @Test
    @DisplayName("savePersonal should update existing layout")
    void savePersonal_existingLayout_updatesCurrentUserLayout() {
        MangoUserGridLayoutEntity existing = new MangoUserGridLayoutEntity();
        existing.setId(10L);
        existing.setTenantId("1");
        existing.setUserId(1001L);
        existing.setPageCode("admin-home-workbench");
        when(gridLayoutMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(gridLayoutMapper.updateById(any(MangoUserGridLayoutEntity.class))).thenReturn(1);

        GridLayoutPersonalVO result = gridLayoutPersonalService.savePersonal(createCommand());

        verify(gridLayoutMapper).updateById(existing);
        verify(gridLayoutMapper, never()).insert(any(MangoUserGridLayoutEntity.class));
        assertEquals(10L, result.getId());
        assertEquals(1, existing.getSchemaVersion());
    }

    @Test
    @DisplayName("savePersonal should reject item wider than grid")
    void savePersonal_itemOverGrid_throwsBizException() {
        SaveGridLayoutPersonalCommand command = createCommand();
        command.setLayoutJson("""
                {"schemaVersion":1,"pageCode":"admin-home-workbench","items":[{"id":"a","widgetType":"todo","layout":{"x":10,"y":0,"w":3,"h":3}}]}
                """);

        assertThrows(BizException.class, () -> gridLayoutPersonalService.savePersonal(command));
    }

    @Test
    @DisplayName("savePersonal should allow item height over twelve rows")
    void savePersonal_itemHeightOverTwelveRows_savesLayout() {
        when(gridLayoutMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(gridLayoutMapper.insert(any(MangoUserGridLayoutEntity.class))).thenReturn(1);
        SaveGridLayoutPersonalCommand command = createCommand();
        command.setLayoutJson("""
                {"schemaVersion":1,"pageCode":"admin-home-workbench","items":[{"id":"a","widgetType":"todo","layout":{"x":0,"y":0,"w":3,"h":24,"minH":2,"maxH":48}}]}
                """);

        GridLayoutPersonalVO result = gridLayoutPersonalService.savePersonal(command);

        assertEquals(command.getLayoutJson(), result.getLayoutJson());
    }

    @Test
    @DisplayName("savePersonal should reject mismatched pageCode")
    void savePersonal_mismatchedPageCode_throwsBizException() {
        SaveGridLayoutPersonalCommand command = createCommand();
        command.setLayoutJson("""
                {"schemaVersion":1,"pageCode":"other-page","items":[]}
                """);

        assertThrows(BizException.class, () -> gridLayoutPersonalService.savePersonal(command));
    }

    @Test
    @DisplayName("deletePersonal should delete current user's layout")
    void deletePersonal_deletesCurrentUserLayout() {
        when(gridLayoutMapper.delete(any(Wrapper.class))).thenReturn(1);

        boolean result = gridLayoutPersonalService.deletePersonal(createQuery());

        assertTrue(result);
        verify(gridLayoutMapper).delete(any(Wrapper.class));
    }

    private SaveGridLayoutPersonalCommand createCommand() {
        SaveGridLayoutPersonalCommand command = new SaveGridLayoutPersonalCommand();
        command.setPageCode("admin-home-workbench");
        command.setLayoutJson("""
                {"schemaVersion":1,"pageCode":"admin-home-workbench","items":[{"id":"a","widgetType":"todo","layout":{"x":0,"y":0,"w":3,"h":3}}]}
                """);
        return command;
    }

    private GridLayoutPersonalQuery createQuery() {
        GridLayoutPersonalQuery query = new GridLayoutPersonalQuery();
        query.setPageCode("admin-home-workbench");
        return query;
    }
}
