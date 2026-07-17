package io.mango.home.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.common.exception.BizException;
import io.mango.home.api.command.CreateHomeTemplateCommand;
import io.mango.home.api.command.HomeTemplateAuthorizationCommand;
import io.mango.home.api.command.SaveHomeTemplateAuthorizationsCommand;
import io.mango.home.api.enums.HomeTemplateAuthorizationSubjectType;
import io.mango.home.api.vo.HomeTemplateAuthorizationVO;
import io.mango.home.api.vo.HomeTemplateVO;
import io.mango.home.core.entity.HomeTemplateEntity;
import io.mango.home.core.entity.HomeTemplateAuthorizationEntity;
import io.mango.home.core.entity.HomeTemplateVersionEntity;
import io.mango.home.core.mapper.HomeTemplateAuthorizationMapper;
import io.mango.home.core.mapper.HomeTemplateMapper;
import io.mango.home.core.mapper.HomeTemplateVersionMapper;
import io.mango.home.core.mapper.UserHomePageMapper;
import io.mango.home.core.mapper.UserHomePreferenceMapper;
import io.mango.home.core.service.IHomeOrgProvider;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeTemplateService Tests")
class HomeTemplateServiceTest {

    private HomeTemplateService service;

    @Mock
    private HomeTemplateMapper templateMapper;

    @Mock
    private HomeTemplateVersionMapper versionMapper;

    @Mock
    private HomeTemplateAuthorizationMapper authorizationMapper;

    @Mock
    private UserHomePageMapper homePageMapper;

    @Mock
    private UserHomePreferenceMapper preferenceMapper;

    @Mock
    private ObjectProvider<IAuthorizationProvider> authorizationProvider;

    @Mock
    private IHomeOrgProvider homeOrgProvider;

    @BeforeEach
    void setUp() {
        service = new HomeTemplateService(templateMapper, versionMapper, authorizationMapper,
                homePageMapper, preferenceMapper, new ObjectMapper(), authorizationProvider, homeOrgProvider);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 10L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void createWithoutLayoutPersistsCanonicalEmptyDraft() {
        when(templateMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(templateMapper.insert(any(HomeTemplateEntity.class))).thenAnswer(invocation -> {
            HomeTemplateEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return 1;
        });
        when(versionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(authorizationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        CreateHomeTemplateCommand command = new CreateHomeTemplateCommand();
        command.setName("运营工作台");

        HomeTemplateVO result = service.create(command);

        ArgumentCaptor<HomeTemplateVersionEntity> versionCaptor =
                ArgumentCaptor.forClass(HomeTemplateVersionEntity.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getTemplateId()).isEqualTo(11L);
        assertThat(versionCaptor.getValue().getLayoutJson())
                .isEqualTo("{\"schemaVersion\":1,\"items\":[]}");
        assertThat(result.getName()).isEqualTo("运营工作台");
    }

    @Test
    void saveEmptyAuthorizationsKeepsClearAllContract() {
        HomeTemplateEntity template = template(11L);
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template);
        when(authorizationMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        SaveHomeTemplateAuthorizationsCommand command = new SaveHomeTemplateAuthorizationsCommand();
        command.setTemplateId(11L);
        command.setAuthorizations(List.of());

        List<HomeTemplateAuthorizationVO> result = service.saveAuthorizations(command);

        assertThat(result).isEmpty();
        verify(authorizationMapper).delete(any(Wrapper.class));
        verify(authorizationMapper, never()).insert(any(HomeTemplateAuthorizationEntity.class));
    }

    @Test
    void roleAuthorizationWithoutCodeIsRejectedBeforeInsert() {
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template(11L));
        SaveHomeTemplateAuthorizationsCommand command = new SaveHomeTemplateAuthorizationsCommand();
        command.setTemplateId(11L);
        HomeTemplateAuthorizationCommand item = new HomeTemplateAuthorizationCommand();
        item.setSubjectType(HomeTemplateAuthorizationSubjectType.ROLE);
        command.setAuthorizations(List.of(item));

        assertThrows(BizException.class, () -> service.saveAuthorizations(command));
        verify(authorizationMapper, never()).insert(any(HomeTemplateAuthorizationEntity.class));
    }

    private HomeTemplateEntity template(Long id) {
        HomeTemplateEntity entity = new HomeTemplateEntity();
        entity.setId(id);
        entity.setTenantId("1");
        entity.setName("运营工作台");
        entity.setEnabled(true);
        entity.setSort(10);
        return entity;
    }
}
