package io.mango.home.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.common.exception.BizException;
import io.mango.home.api.command.CreateHomePageCommand;
import io.mango.home.api.command.SaveHomePageLayoutCommand;
import io.mango.home.api.command.SortHomePagesCommand;
import io.mango.home.api.query.ResolveHomePageQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.core.mapper.HomeTemplateAuthorizationMapper;
import io.mango.home.core.mapper.HomeTemplateMapper;
import io.mango.home.core.mapper.HomeTemplateVersionMapper;
import io.mango.home.core.entity.UserHomePageEntity;
import io.mango.home.core.entity.UserHomePreferenceEntity;
import io.mango.home.core.integration.HomeOrgGateway;
import io.mango.home.core.mapper.UserHomePageMapper;
import io.mango.home.core.mapper.UserHomePreferenceMapper;
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
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomePageService Tests")
class HomePageServiceTest {

    @Mock
    private UserHomePageMapper homePageMapper;

    @Mock
    private UserHomePreferenceMapper preferenceMapper;

    @Mock
    private HomeTemplateMapper templateMapper;

    @Mock
    private HomeTemplateVersionMapper templateVersionMapper;

    @Mock
    private HomeTemplateAuthorizationMapper templateAuthorizationMapper;

    @Mock
    private ObjectProvider<IAuthorizationProvider> authorizationProvider;

    private final HomeOrgGateway homeOrgGateway = mock(HomeOrgGateway.class);

    private HomePageService homePageService;

    @BeforeEach
    void setUp() {
        homePageService = new HomePageService(homePageMapper, preferenceMapper, templateMapper, templateVersionMapper,
                templateAuthorizationMapper, new ObjectMapper(), authorizationProvider, homeOrgGateway);
        lenient().when(templateAuthorizationMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("resolve should return built-in page when user has no page")
    void resolve_noPersonalPage_returnsBuiltInDefault() {
        when(preferenceMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(homePageMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        HomePageVO result = homePageService.resolve(new ResolveHomePageQuery());

        assertTrue(result.getBuiltIn());
        assertTrue(result.getDefaultPage());
        assertNull(result.getId());
        assertEquals("系统工作台", result.getName());
    }

    @Test
    @DisplayName("create should insert current user page and create default preference")
    void create_firstPage_insertsAndSetsDefault() {
        when(homePageMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(preferenceMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(homePageMapper.insert(any(UserHomePageEntity.class))).thenAnswer(invocation -> {
            UserHomePageEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return 1;
        });
        when(preferenceMapper.insert(any(UserHomePreferenceEntity.class))).thenReturn(1);

        HomePageVO result = homePageService.create(createCommand("销售工作台"));

        ArgumentCaptor<UserHomePageEntity> pageCaptor = ArgumentCaptor.forClass(UserHomePageEntity.class);
        verify(homePageMapper).insert(pageCaptor.capture());
        UserHomePageEntity entity = pageCaptor.getValue();
        assertEquals("1", entity.getTenantId());
        assertEquals(1001L, entity.getUserId());
        assertEquals("销售工作台", entity.getName());
        assertEquals(10, entity.getSort());
        assertTrue(result.getDefaultPage());
        verify(preferenceMapper).insert(any(UserHomePreferenceEntity.class));
    }

    @Test
    @DisplayName("resolve should open owned specified page")
    void resolve_specifiedOwnedPage_returnsPage() {
        UserHomePageEntity page = page(20L, "项目工作台", 10);
        UserHomePreferenceEntity preference = preference(20L);
        when(homePageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(page));
        when(preferenceMapper.selectOne(any(Wrapper.class))).thenReturn(preference);
        ResolveHomePageQuery query = new ResolveHomePageQuery();
        query.setHomeId("20");

        HomePageVO result = homePageService.resolve(query);

        assertEquals(20L, result.getId());
        assertTrue(result.getDefaultPage());
    }

    @Test
    @DisplayName("delete should fallback to next enabled page when default page is deleted")
    void delete_defaultPage_selectsFallbackDefault() {
        UserHomePageEntity deleting = page(20L, "默认工作台", 10);
        UserHomePageEntity fallback = page(30L, "备用工作台", 20);
        UserHomePreferenceEntity preference = preference(20L);
        when(homePageMapper.selectOne(any(Wrapper.class))).thenReturn(deleting, fallback);
        when(preferenceMapper.selectOne(any(Wrapper.class))).thenReturn(preference);
        when(homePageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(fallback));
        when(homePageMapper.updateById(any(UserHomePageEntity.class))).thenReturn(1);
        when(preferenceMapper.updateById(any(UserHomePreferenceEntity.class))).thenReturn(1);

        HomePageVO result = homePageService.delete(20L);

        assertEquals(30L, result.getId());
        assertTrue(result.getDefaultPage());
        assertFalse(deleting.getEnabled());
        assertEquals(30L, preference.getDefaultHomePageId());
    }

    @Test
    @DisplayName("sort should reject page id that current user does not own")
    void sort_foreignPageId_throwsBizException() {
        when(homePageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(page(20L, "默认工作台", 10)));
        SortHomePagesCommand command = new SortHomePagesCommand();
        command.setIds(List.of(20L, 99L));

        assertThrows(BizException.class, () -> homePageService.sort(command));
        verify(homePageMapper, never()).updateById(any(UserHomePageEntity.class));
    }

    @Test
    @DisplayName("saveLayout should reject invalid layout")
    void saveLayout_invalidLayout_throwsBizException() {
        SaveHomePageLayoutCommand command = new SaveHomePageLayoutCommand();
        command.setLayoutJson("""
                {"schemaVersion":1,"items":[{"id":"a","widgetType":"todo","layout":{"x":10,"y":0,"w":3,"h":3}}]}
                """);

        assertThrows(BizException.class, () -> homePageService.saveLayout(20L, command));
    }

    @Test
    @DisplayName("duplicate should copy source layout and append suffix")
    void duplicate_ownedPage_copiesLayout() {
        UserHomePageEntity source = page(20L, "流程工作台", 10);
        when(homePageMapper.selectOne(any(Wrapper.class))).thenReturn(source);
        when(homePageMapper.selectList(any(Wrapper.class))).thenReturn(new ArrayList<>(List.of(source)));
        when(homePageMapper.insert(any(UserHomePageEntity.class))).thenAnswer(invocation -> {
            UserHomePageEntity entity = invocation.getArgument(0);
            entity.setId(21L);
            return 1;
        });

        HomePageVO result = homePageService.duplicate(20L);

        assertEquals(21L, result.getId());
        assertEquals("流程工作台 副本", result.getName());
        assertFalse(result.getDefaultPage());
    }

    private CreateHomePageCommand createCommand(String name) {
        CreateHomePageCommand command = new CreateHomePageCommand();
        command.setName(name);
        command.setLayoutJson("""
                {"schemaVersion":1,"items":[{"id":"a","widgetType":"todo","layout":{"x":0,"y":0,"w":3,"h":3}}]}
                """);
        return command;
    }

    private UserHomePageEntity page(Long id, String name, Integer sort) {
        UserHomePageEntity entity = new UserHomePageEntity();
        entity.setId(id);
        entity.setTenantId("1");
        entity.setUserId(1001L);
        entity.setName(name);
        entity.setLayoutJson("""
                {"schemaVersion":1,"items":[{"id":"a","widgetType":"todo","layout":{"x":0,"y":0,"w":3,"h":3}}]}
                """);
        entity.setSort(sort);
        entity.setEnabled(true);
        return entity;
    }

    private UserHomePreferenceEntity preference(Long defaultHomePageId) {
        UserHomePreferenceEntity preference = new UserHomePreferenceEntity();
        preference.setId(1L);
        preference.setTenantId("1");
        preference.setUserId(1001L);
        preference.setDefaultHomePageId(defaultHomePageId);
        return preference;
    }
}
