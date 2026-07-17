package io.mango.link.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.common.exception.BizException;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.link.api.enums.LinkNavigationSource;
import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkPublicItemVO;
import io.mango.link.core.entity.LinkCategoryEntity;
import io.mango.link.core.entity.LinkFavoriteEntity;
import io.mango.link.core.entity.LinkItemEntity;
import io.mango.link.core.integration.LinkConfigGateway;
import io.mango.link.core.mapper.LinkAccessRecordMapper;
import io.mango.link.core.mapper.LinkCategoryMapper;
import io.mango.link.core.mapper.LinkFavoriteMapper;
import io.mango.link.core.mapper.LinkItemMapper;
import io.mango.link.core.service.LinkRedirectContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        LinkServiceIntegrationTest.TestConfiguration.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:link_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@Sql(scripts = "classpath:db/migration/link/V1__init_link.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class LinkServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LinkOpenService linkOpenService;

    @Autowired
    private LinkCategoryMapper categoryMapper;

    @Autowired
    private LinkItemMapper itemMapper;

    @Autowired
    private LinkFavoriteMapper favoriteMapper;

    @Autowired
    private LinkAccessRecordMapper accessRecordMapper;

    @MockitoBean
    private TenantMemberProvider tenantMemberProvider;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from link_access_record");
        jdbcTemplate.update("delete from link_favorite");
        jdbcTemplate.update("delete from link_visibility_target");
        jdbcTemplate.update("delete from link_item");
        jdbcTemplate.update("delete from link_category");
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        TenantMemberVO member = new TenantMemberVO();
        member.setMemberId(10L);
        member.setTenantId(1L);
        member.setUserId(1L);
        member.setDisplayName("管理员");
        member.setStatus(1);
        when(tenantMemberProvider.getEnabledMember(1L, 1L)).thenReturn(member);
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void publicAndAuthenticatedQueriesKeepTheirAccessBoundariesWithRealMappers() {
        LinkCategoryEntity companyCategory = category(100L, "COMPANY", 0L, "企业导航");
        LinkCategoryEntity personalCategory = category(101L, "PERSONAL", 1L, "我的分组");
        categoryMapper.insert(companyCategory);
        categoryMapper.insert(personalCategory);

        itemMapper.insert(item(200L, 100L, "公开网址", "PUBLIC", 0L, "https://public.example.com"));
        itemMapper.insert(item(201L, 100L, "企业网址", "COMPANY", 0L, "https://company.example.com"));
        itemMapper.insert(item(202L, 101L, "个人网址", "PERSONAL", 1L, "https://personal.example.com"));
        itemMapper.insert(item(203L, 101L, "他人网址", "PERSONAL", 2L, "https://other.example.com"));
        LinkFavoriteEntity favorite = new LinkFavoriteEntity();
        favorite.setId(300L);
        favorite.setTenantId("1");
        favorite.setOrgId(1L);
        favorite.setUserId(1L);
        favorite.setLinkId(201L);
        favoriteMapper.insert(favorite);

        List<LinkPublicItemVO> publicItems = linkOpenService.listPublicItems(new LinkPublicItemQuery());
        List<LinkPublicItemVO> visibleItems = linkOpenService.listVisibleItems(new LinkPublicItemQuery());

        assertThat(publicItems).extracting(LinkPublicItemVO::getName).containsExactly("公开网址");
        assertThat(visibleItems).extracting(LinkPublicItemVO::getName)
                .containsExactly("公开网址", "企业网址", "企业网址", "个人网址");
        assertThat(visibleItems).extracting(LinkPublicItemVO::getSource)
                .containsExactly(LinkNavigationSource.COMPANY, LinkNavigationSource.COMPANY,
                        LinkNavigationSource.FAVORITE, LinkNavigationSource.PERSONAL);
    }

    @Test
    void redirectPersistsAccessAndRejectsCrossTenantLookupWithRealMappers() {
        categoryMapper.insert(category(110L, "COMPANY", 0L, "公开分类"));
        itemMapper.insert(item(210L, 110L, "公开网址", "PUBLIC", 0L, "https://target.example.com"));
        LinkRedirectContext context = LinkRedirectContext.builder()
                .id(210L)
                .source("PUBLIC")
                .clientIp("127.0.0.1")
                .userAgent("integration-test")
                .referer("https://origin.example.com")
                .build();

        assertThat(linkOpenService.resolveRedirectUrl(context)).isEqualTo("https://target.example.com");
        assertThat(accessRecordMapper.selectCount(null)).isEqualTo(1L);

        MangoContextHolder.clear();
        assertThat(linkOpenService.resolveRedirectUrl(context)).isEqualTo("https://target.example.com");
        assertThat(accessRecordMapper.selectCount(null)).isEqualTo(2L);

        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L, "2", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 2L, "internal-admin"));
        assertThatThrownBy(() -> linkOpenService.resolveRedirectUrl(context))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("网址不存在");
    }

    private LinkCategoryEntity category(Long id, String scope, Long ownerUserId, String name) {
        LinkCategoryEntity entity = new LinkCategoryEntity();
        entity.setId(id);
        entity.setTenantId("1");
        entity.setOrgId(1L);
        entity.setScope(scope);
        entity.setOwnerUserId(ownerUserId);
        entity.setName(name);
        entity.setSortNo(0);
        entity.setStatus("ENABLED");
        return entity;
    }

    private LinkItemEntity item(Long id,
                                Long categoryId,
                                String name,
                                String visibilityScope,
                                Long ownerUserId,
                                String url) {
        LinkItemEntity entity = new LinkItemEntity();
        entity.setId(id);
        entity.setTenantId("1");
        entity.setOrgId(1L);
        entity.setCategoryId(categoryId);
        entity.setName(name);
        entity.setUrl(url);
        entity.setVisibilityScope(visibilityScope);
        entity.setOwnerUserId(ownerUserId);
        entity.setOpenMode("NEW_WINDOW");
        entity.setRecommended(false);
        entity.setSortNo(0);
        entity.setStatus("ENABLED");
        return entity;
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan("io.mango.link.core.mapper")
    @Import({
            LinkAdminService.class,
            LinkOpenService.class,
            LinkUserService.class,
            LinkServiceSupport.class,
            LinkConfigGateway.class
    })
    static class TestConfiguration {
    }
}
