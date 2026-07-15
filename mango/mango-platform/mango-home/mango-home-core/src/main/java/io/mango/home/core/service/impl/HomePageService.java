package io.mango.home.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.home.api.command.BatchDeleteHomePagesCommand;
import io.mango.home.api.command.CreateHomePageCommand;
import io.mango.home.api.command.RenameHomePageCommand;
import io.mango.home.api.command.SaveHomePageLayoutCommand;
import io.mango.home.api.command.SetDefaultHomePageCommand;
import io.mango.home.api.command.SortHomePagesCommand;
import io.mango.home.api.enums.HomePageSourceType;
import io.mango.home.api.enums.HomeTemplateAuthorizationSubjectType;
import io.mango.home.api.enums.HomeTemplateVersionStatus;
import io.mango.home.api.query.ResolveHomePageQuery;
import io.mango.home.api.query.UserHomePageQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.core.entity.HomeTemplateAuthorizationEntity;
import io.mango.home.core.entity.HomeTemplateEntity;
import io.mango.home.core.entity.HomeTemplateVersionEntity;
import io.mango.home.core.entity.UserHomePageEntity;
import io.mango.home.core.entity.UserHomePreferenceEntity;
import io.mango.home.core.mapper.HomeTemplateAuthorizationMapper;
import io.mango.home.core.mapper.HomeTemplateMapper;
import io.mango.home.core.mapper.HomeTemplateVersionMapper;
import io.mango.home.core.mapper.UserHomePageMapper;
import io.mango.home.core.mapper.UserHomePreferenceMapper;
import io.mango.home.core.service.IHomePageService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.entity.SysOrg;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomePageService implements IHomePageService {

    private static final String BUILT_IN_NAME = "系统工作台";
    private static final String DUPLICATE_SUFFIX = " 副本";
    private static final int DEFAULT_SORT_STEP = 10;

    private final UserHomePageMapper homePageMapper;
    private final UserHomePreferenceMapper preferenceMapper;
    private final HomeTemplateMapper templateMapper;
    private final HomeTemplateVersionMapper templateVersionMapper;
    private final HomeTemplateAuthorizationMapper templateAuthorizationMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<IAuthorizationProvider> authorizationProvider;
    private final ObjectProvider<SysOrgApi> sysOrgApiProvider;

    @Override
    public List<HomePageVO> listMyPages() {
        return visiblePages(currentPreference());
    }

    @Override
    public PageResult<HomePageVO> pageUserPages(UserHomePageQuery query) {
        UserHomePageQuery resolved = query == null ? new UserHomePageQuery() : query;
        IPage<UserHomePageEntity> page = homePageMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                userHomePageWrapper(resolved));
        return PageResult.of(page.getRecords().stream()
                        .map(entity -> toVO(entity, false))
                        .toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public HomePageVO resolve(ResolveHomePageQuery query) {
        String routeKey = query == null ? null : query.getHomeId();
        UserHomePreferenceEntity preference = currentPreference();
        List<HomePageVO> pages = visiblePages(preference);
        if (routeKey != null && !routeKey.isBlank()) {
            HomePageVO specified = pages.stream()
                    .filter(page -> routeKey.equals(page.getRouteKey()) || routeKey.equals(String.valueOf(page.getId())))
                    .findFirst()
                    .orElse(null);
            Require.notNull(specified, "首页不存在或无权访问");
            return specified;
        }
        HomePageVO resolved = resolveDefaultPage(pages, preference);
        return resolved == null ? builtInDefault() : resolved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO create(CreateHomePageCommand command) {
        Require.notNull(command, "创建命令不能为空");
        Require.notBlank(command.getName(), "首页名称不能为空");
        String layoutJson = HomeLayoutSupport.normalize(objectMapper, command.getLayoutJson());
        int nextSort = nextSort();
        UserHomePageEntity entity = new UserHomePageEntity();
        entity.setTenantId(HomeContextSupport.currentTenantId());
        entity.setOrgId(HomeContextSupport.currentOrgId());
        entity.setUserId(HomeContextSupport.currentUserId());
        entity.setName(command.getName().trim());
        entity.setLayoutJson(layoutJson);
        entity.setSort(nextSort);
        entity.setEnabled(true);
        boolean setAsDefault = Boolean.TRUE.equals(command.getSetDefault()) || currentDefaultHomeRef() == null;
        homePageMapper.insert(entity);
        if (setAsDefault) {
            saveDefaultHomeRef(HomeRouteKeys.user(entity.getId()), entity.getId());
        }
        return toVO(entity, setAsDefault);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO rename(Long id, RenameHomePageCommand command) {
        Require.notNull(command, "重命名命令不能为空");
        Require.notBlank(command.getName(), "首页名称不能为空");
        UserHomePageEntity entity = requiredOwnedEnabled(id);
        entity.setName(command.getName().trim());
        entity.setUpdatedBy(MangoContextHolder.userId());
        homePageMapper.updateById(entity);
        return toVO(entity, isDefaultRoute(HomeRouteKeys.user(entity.getId()), currentPreference()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO duplicate(Long id) {
        UserHomePageEntity source = requiredOwnedEnabled(id);
        UserHomePageEntity entity = new UserHomePageEntity();
        entity.setTenantId(HomeContextSupport.currentTenantId());
        entity.setOrgId(HomeContextSupport.currentOrgId());
        entity.setUserId(HomeContextSupport.currentUserId());
        entity.setName(source.getName() + DUPLICATE_SUFFIX);
        entity.setLayoutJson(source.getLayoutJson());
        entity.setSort(nextSort());
        entity.setEnabled(true);
        homePageMapper.insert(entity);
        return toVO(entity, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO saveLayout(Long id, SaveHomePageLayoutCommand command) {
        Require.notNull(command, "布局保存命令不能为空");
        Require.notBlank(command.getLayoutJson(), "layoutJson不能为空");
        HomeLayoutSupport.validate(objectMapper, command.getLayoutJson());
        UserHomePageEntity entity = requiredOwnedEnabled(id);
        entity.setLayoutJson(command.getLayoutJson());
        entity.setUpdatedBy(MangoContextHolder.userId());
        homePageMapper.updateById(entity);
        return toVO(entity, isDefaultRoute(HomeRouteKeys.user(entity.getId()), currentPreference()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<HomePageVO> sort(SortHomePagesCommand command) {
        Require.notNull(command, "排序命令不能为空");
        Require.notEmpty(command.getIds(), "首页排序不能为空");
        List<UserHomePageEntity> pages = listEnabledEntities();
        Set<Long> ownedIds = new LinkedHashSet<>();
        for (UserHomePageEntity page : pages) {
            ownedIds.add(page.getId());
        }
        int sort = DEFAULT_SORT_STEP;
        for (Long id : command.getIds()) {
            Require.isTrue(ownedIds.contains(id), "首页排序包含无权访问的数据");
            UserHomePageEntity entity = requiredOwnedEnabled(id);
            entity.setSort(sort);
            entity.setUpdatedBy(MangoContextHolder.userId());
            homePageMapper.updateById(entity);
            sort += DEFAULT_SORT_STEP;
        }
        return listMyPages();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO setDefault(SetDefaultHomePageCommand command) {
        Require.notNull(command, "默认首页命令不能为空");
        Require.notBlank(command.getHomeId(), "首页标识不能为空");
        List<HomePageVO> pages = visiblePages(currentPreference());
        HomePageVO page = pages.stream()
                .filter(item -> command.getHomeId().equals(item.getRouteKey()))
                .findFirst()
                .orElse(null);
        Require.notNull(page, "首页不存在或无权访问");
        Long personalId = HomeRouteKeys.parseUserPageId(page.getRouteKey());
        saveDefaultHomeRef(page.getRouteKey(), personalId);
        page.setDefaultPage(true);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO delete(Long id) {
        UserHomePageEntity entity = requiredOwnedEnabled(id);
        entity.setEnabled(false);
        entity.setUpdatedBy(MangoContextHolder.userId());
        homePageMapper.updateById(entity);
        UserHomePreferenceEntity preference = currentPreference();
        if (isDefaultRoute(HomeRouteKeys.user(id), preference)) {
            List<HomePageVO> pages = visiblePages(preference).stream()
                    .filter(page -> !HomeRouteKeys.user(id).equals(page.getRouteKey()))
                    .toList();
            HomePageVO fallback = pages.isEmpty() ? null : pages.get(0);
            saveDefaultHomeRef(fallback == null ? null : fallback.getRouteKey(),
                    fallback == null ? null : HomeRouteKeys.parseUserPageId(fallback.getRouteKey()));
        }
        return resolve(new ResolveHomePageQuery());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO adminRename(Long id, RenameHomePageCommand command) {
        Require.notNull(command, "重命名命令不能为空");
        Require.notBlank(command.getName(), "首页名称不能为空");
        UserHomePageEntity entity = requiredTenantEnabled(id);
        entity.setName(command.getName().trim());
        entity.setUpdatedBy(MangoContextHolder.userId());
        homePageMapper.updateById(entity);
        return toVO(entity, isDefaultRoute(HomeRouteKeys.user(entity.getId()), preferenceForUser(entity.getUserId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePageVO adminSaveLayout(Long id, SaveHomePageLayoutCommand command) {
        Require.notNull(command, "布局保存命令不能为空");
        Require.notBlank(command.getLayoutJson(), "layoutJson不能为空");
        HomeLayoutSupport.validate(objectMapper, command.getLayoutJson());
        UserHomePageEntity entity = requiredTenantEnabled(id);
        entity.setLayoutJson(command.getLayoutJson());
        entity.setUpdatedBy(MangoContextHolder.userId());
        homePageMapper.updateById(entity);
        return toVO(entity, isDefaultRoute(HomeRouteKeys.user(entity.getId()), preferenceForUser(entity.getUserId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Long id) {
        deleteAdminPage(requiredTenantPage(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminBatchDelete(BatchDeleteHomePagesCommand command) {
        Require.notNull(command, "批量删除命令不能为空");
        Require.notEmpty(command.getIds(), "首页ID不能为空");
        for (Long id : new LinkedHashSet<>(command.getIds())) {
            deleteAdminPage(requiredTenantPage(id));
        }
    }

    private List<HomePageVO> visiblePages(UserHomePreferenceEntity preference) {
        List<HomePageVO> result = new ArrayList<>();
        for (UserHomePageEntity page : listEnabledEntities()) {
            result.add(toVO(page, isDefaultRoute(HomeRouteKeys.user(page.getId()), preference)));
        }
        result.addAll(authorizedPages(preference));
        if (result.isEmpty()) {
            result.add(builtInDefault());
        }
        applyDefaultSelection(result, preference);
        return result;
    }

    private List<HomePageVO> authorizedPages(UserHomePreferenceEntity preference) {
        String tenantId = HomeContextSupport.currentTenantId();
        Map<Long, AuthorizedTemplateMatch> matches = new LinkedHashMap<>();
        addAuthorizationMatches(matches, listUserAuthorizations(tenantId));
        addAuthorizationMatches(matches, listOrgAuthorizations(tenantId));
        addAuthorizationMatches(matches, listRoleAuthorizations(tenantId));
        List<HomePageVO> result = new ArrayList<>();
        for (AuthorizedTemplateMatch match : matches.values()) {
            HomeTemplateEntity template = templateMapper.selectOne(new LambdaQueryWrapper<HomeTemplateEntity>()
                    .eq(HomeTemplateEntity::getTenantId, tenantId)
                    .eq(HomeTemplateEntity::getId, match.templateId())
                    .eq(HomeTemplateEntity::getEnabled, true));
            if (template == null || template.getActiveVersionId() == null) {
                continue;
            }
            HomeTemplateVersionEntity version = templateVersionMapper.selectOne(new LambdaQueryWrapper<HomeTemplateVersionEntity>()
                    .eq(HomeTemplateVersionEntity::getTenantId, tenantId)
                    .eq(HomeTemplateVersionEntity::getId, template.getActiveVersionId())
                    .eq(HomeTemplateVersionEntity::getStatus, HomeTemplateVersionStatus.ACTIVE.name()));
            if (version == null) {
                continue;
            }
            result.add(toAuthorizedVO(template, version, match, isDefaultRoute(HomeRouteKeys.template(template.getId()), preference)));
        }
        result.sort(Comparator.comparing(HomePageVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(HomePageVO::getName, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private List<HomeTemplateAuthorizationEntity> listUserAuthorizations(String tenantId) {
        return templateAuthorizationMapper.selectList(baseAuthorizationWrapper(tenantId)
                .eq(HomeTemplateAuthorizationEntity::getSubjectType, HomeTemplateAuthorizationSubjectType.USER.name())
                .eq(HomeTemplateAuthorizationEntity::getSubjectId, HomeContextSupport.currentUserId()));
    }

    private List<HomeTemplateAuthorizationEntity> listOrgAuthorizations(String tenantId) {
        Set<Long> orgIds = currentOrgAndAncestors();
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return templateAuthorizationMapper.selectList(baseAuthorizationWrapper(tenantId)
                .eq(HomeTemplateAuthorizationEntity::getSubjectType, HomeTemplateAuthorizationSubjectType.ORG.name())
                .in(HomeTemplateAuthorizationEntity::getSubjectId, orgIds));
    }

    private List<HomeTemplateAuthorizationEntity> listRoleAuthorizations(String tenantId) {
        Set<String> roleCodes = currentRoleCodes();
        if (roleCodes.isEmpty()) {
            return List.of();
        }
        return templateAuthorizationMapper.selectList(baseAuthorizationWrapper(tenantId)
                .eq(HomeTemplateAuthorizationEntity::getSubjectType, HomeTemplateAuthorizationSubjectType.ROLE.name())
                .in(HomeTemplateAuthorizationEntity::getSubjectCode, roleCodes));
    }

    private LambdaQueryWrapper<HomeTemplateAuthorizationEntity> baseAuthorizationWrapper(String tenantId) {
        return new LambdaQueryWrapper<HomeTemplateAuthorizationEntity>()
                .eq(HomeTemplateAuthorizationEntity::getTenantId, tenantId)
                .eq(HomeTemplateAuthorizationEntity::getEnabled, true)
                .orderByAsc(HomeTemplateAuthorizationEntity::getSort)
                .orderByAsc(HomeTemplateAuthorizationEntity::getCreatedAt);
    }

    private void addAuthorizationMatches(Map<Long, AuthorizedTemplateMatch> matches,
                                         List<HomeTemplateAuthorizationEntity> authorizations) {
        for (HomeTemplateAuthorizationEntity authorization : authorizations) {
            matches.compute(authorization.getTemplateId(), (templateId, existing) -> {
                AuthorizedTemplateMatch match = existing == null
                        ? new AuthorizedTemplateMatch(templateId, authorization.getDefaultFlag(), new ArrayList<>())
                        : existing;
                match.sourceLabels().add(sourceLabel(authorization));
                if (Boolean.TRUE.equals(authorization.getDefaultFlag())) {
                    match = new AuthorizedTemplateMatch(templateId, true, match.sourceLabels());
                }
                return match;
            });
        }
    }

    private String sourceLabel(HomeTemplateAuthorizationEntity authorization) {
        String name = authorization.getSubjectName();
        if (name == null || name.isBlank()) {
            name = authorization.getSubjectCode() != null ? authorization.getSubjectCode() : String.valueOf(authorization.getSubjectId());
        }
        return switch (HomeTemplateAuthorizationSubjectType.valueOf(authorization.getSubjectType())) {
            case USER -> "个人授权：" + name;
            case ORG -> "部门授权：" + name;
            case ROLE -> "角色授权：" + name;
        };
    }

    private Set<Long> currentOrgAndAncestors() {
        LinkedHashSet<Long> orgIds = new LinkedHashSet<>();
        Long currentOrgId = HomeContextSupport.currentOrgId();
        if (currentOrgId == null) {
            return orgIds;
        }
        orgIds.add(currentOrgId);
        SysOrgApi sysOrgApi = sysOrgApiProvider.getIfAvailable();
        if (sysOrgApi == null) {
            return orgIds;
        }
        Long cursor = currentOrgId;
        while (cursor != null && cursor > 0) {
            R<SysOrg> response = sysOrgApi.getById(cursor);
            SysOrg org = response == null ? null : response.getData();
            if (org == null || org.getPid() == null || org.getPid() <= 0 || orgIds.contains(org.getPid())) {
                break;
            }
            orgIds.add(org.getPid());
            cursor = org.getPid();
        }
        return orgIds;
    }

    private Set<String> currentRoleCodes() {
        IAuthorizationProvider provider = authorizationProvider.getIfAvailable();
        if (provider == null || MangoContextHolder.memberId() == null) {
            return Set.of();
        }
        AuthorizationQuery query = AuthorizationQuery.member(MangoContextHolder.memberId())
                .withTenantId(MangoContextHolder.tenantId())
                .withSystemCode(MangoContextHolder.appCode())
                .withRealm(MangoContextHolder.get().realm())
                .withActorType(MangoContextHolder.get().actorType())
                .withParty(MangoContextHolder.get().partyType(), MangoContextHolder.get().partyId());
        AuthorizationSnapshotVO snapshot = provider.load(query);
        return snapshot == null ? Set.of() : snapshot.roleCodes();
    }

    private HomePageVO resolveDefaultPage(List<HomePageVO> pages, UserHomePreferenceEntity preference) {
        applyDefaultSelection(pages, preference);
        return pages.stream()
                .filter(page -> Boolean.TRUE.equals(page.getDefaultPage()))
                .findFirst()
                .orElse(null);
    }

    private void applyDefaultSelection(List<HomePageVO> pages, UserHomePreferenceEntity preference) {
        if (pages.isEmpty()) {
            return;
        }
        Set<String> authorizationDefaultRoutes = pages.stream()
                .filter(page -> Boolean.TRUE.equals(page.getDefaultPage()))
                .map(HomePageVO::getRouteKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        pages.forEach(page -> page.setDefaultPage(false));
        if (preference != null && preference.getDefaultHomeRef() != null) {
            HomePageVO preferred = findByRoute(pages, preference.getDefaultHomeRef());
            if (preferred != null) {
                preferred.setDefaultPage(true);
                return;
            }
        }
        if (preference != null && preference.getDefaultHomePageId() != null) {
            HomePageVO preferred = findByRoute(pages, HomeRouteKeys.user(preference.getDefaultHomePageId()));
            if (preferred != null) {
                preferred.setDefaultPage(true);
                return;
            }
        }
        HomePageVO authDefault = pages.stream()
                .filter(page -> HomePageSourceType.PERSONAL_AUTH.name().equals(page.getSourceType()))
                .filter(page -> authorizationDefaultRoutes.contains(page.getRouteKey()))
                .findFirst()
                .orElse(null);
        if (authDefault == null) {
            authDefault = pages.stream()
                    .filter(page -> HomePageSourceType.ORG_AUTH.name().equals(page.getSourceType()))
                    .filter(page -> authorizationDefaultRoutes.contains(page.getRouteKey()))
                    .findFirst()
                    .orElse(null);
        }
        if (authDefault == null) {
            authDefault = pages.stream()
                    .filter(page -> HomePageSourceType.ROLE_AUTH.name().equals(page.getSourceType()))
                    .filter(page -> authorizationDefaultRoutes.contains(page.getRouteKey()))
                    .findFirst()
                    .orElse(null);
        }
        HomePageVO selected = authDefault == null ? pages.get(0) : authDefault;
        selected.setDefaultPage(true);
    }

    private HomePageVO findByRoute(List<HomePageVO> pages, String routeKey) {
        return pages.stream()
                .filter(page -> routeKey.equals(page.getRouteKey()))
                .findFirst()
                .orElse(null);
    }

    private boolean isDefaultRoute(String routeKey, UserHomePreferenceEntity preference) {
        if (routeKey == null || preference == null) {
            return false;
        }
        if (preference.getDefaultHomeRef() != null) {
            return routeKey.equals(preference.getDefaultHomeRef());
        }
        return routeKey.equals(HomeRouteKeys.user(preference.getDefaultHomePageId()));
    }

    private UserHomePageEntity requiredOwnedEnabled(Long id) {
        Require.notNull(id, "首页ID不能为空");
        UserHomePageEntity entity = selectOwnedEnabled(id);
        Require.notNull(entity, "首页不存在或无权访问");
        return entity;
    }

    private UserHomePageEntity requiredTenantEnabled(Long id) {
        Require.notNull(id, "首页ID不能为空");
        UserHomePageEntity entity = homePageMapper.selectOne(tenantWrapper()
                .eq(UserHomePageEntity::getId, id)
                .eq(UserHomePageEntity::getEnabled, true));
        Require.notNull(entity, "首页不存在或无权访问");
        return entity;
    }

    private UserHomePageEntity requiredTenantPage(Long id) {
        Require.notNull(id, "首页ID不能为空");
        UserHomePageEntity entity = homePageMapper.selectOne(tenantWrapper()
                .eq(UserHomePageEntity::getId, id));
        Require.notNull(entity, "首页不存在或无权访问");
        return entity;
    }

    private void deleteAdminPage(UserHomePageEntity entity) {
        clearDeletedDefaultRef(entity);
        homePageMapper.delete(tenantWrapper().eq(UserHomePageEntity::getId, entity.getId()));
    }

    private void clearDeletedDefaultRef(UserHomePageEntity entity) {
        UserHomePreferenceEntity preference = preferenceForUser(entity.getUserId());
        if (!isDefaultRoute(HomeRouteKeys.user(entity.getId()), preference)) {
            return;
        }
        preference.setDefaultHomePageId(null);
        preference.setDefaultHomeRef(null);
        preference.setUpdatedBy(MangoContextHolder.userId());
        preferenceMapper.updateById(preference);
    }

    private UserHomePageEntity selectOwnedEnabled(Long id) {
        return homePageMapper.selectOne(baseWrapper()
                .eq(UserHomePageEntity::getId, id)
                .eq(UserHomePageEntity::getEnabled, true));
    }

    private List<UserHomePageEntity> listEnabledEntities() {
        return homePageMapper.selectList(baseWrapper()
                .eq(UserHomePageEntity::getEnabled, true)
                .orderByAsc(UserHomePageEntity::getSort)
                .orderByAsc(UserHomePageEntity::getCreatedAt)
                .orderByAsc(UserHomePageEntity::getId));
    }

    private int nextSort() {
        List<UserHomePageEntity> pages = listEnabledEntities();
        if (pages.isEmpty()) {
            return DEFAULT_SORT_STEP;
        }
        return pages.get(pages.size() - 1).getSort() + DEFAULT_SORT_STEP;
    }

    private LambdaQueryWrapper<UserHomePageEntity> baseWrapper() {
        return new LambdaQueryWrapper<UserHomePageEntity>()
                .eq(UserHomePageEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(UserHomePageEntity::getUserId, HomeContextSupport.currentUserId());
    }

    private LambdaQueryWrapper<UserHomePageEntity> tenantWrapper() {
        return new LambdaQueryWrapper<UserHomePageEntity>()
                .eq(UserHomePageEntity::getTenantId, HomeContextSupport.currentTenantId());
    }

    private LambdaQueryWrapper<UserHomePageEntity> userHomePageWrapper(UserHomePageQuery query) {
        String keyword = query.getKeyword() == null ? null : query.getKeyword().trim();
        String routeKeyword = routeKeyword(keyword);
        LambdaQueryWrapper<UserHomePageEntity> wrapper = new LambdaQueryWrapper<UserHomePageEntity>()
                .eq(UserHomePageEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(query.getUserId() != null, UserHomePageEntity::getUserId, query.getUserId())
                .eq(query.getEnabled() != null, UserHomePageEntity::getEnabled, query.getEnabled());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(UserHomePageEntity::getName, keyword)
                .or(StringUtils.hasText(routeKeyword))
                .like(StringUtils.hasText(routeKeyword), UserHomePageEntity::getId, routeKeyword));
        wrapper.orderByDesc(UserHomePageEntity::getUpdatedAt)
                .orderByDesc(UserHomePageEntity::getCreatedAt)
                .orderByDesc(UserHomePageEntity::getId);
        return wrapper;
    }

    private String routeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.startsWith("user:") ? keyword.substring("user:".length()) : keyword;
    }

    private UserHomePreferenceEntity currentPreference() {
        return preferenceMapper.selectOne(preferenceWrapper());
    }

    private String currentDefaultHomeRef() {
        UserHomePreferenceEntity preference = currentPreference();
        if (preference == null) {
            return null;
        }
        return preference.getDefaultHomeRef() == null
                ? HomeRouteKeys.user(preference.getDefaultHomePageId())
                : preference.getDefaultHomeRef();
    }

    private void saveDefaultHomeRef(String homeRef, Long homePageId) {
        UserHomePreferenceEntity preference = preferenceMapper.selectOne(preferenceWrapper());
        if (preference == null) {
            preference = new UserHomePreferenceEntity();
            preference.setTenantId(HomeContextSupport.currentTenantId());
            preference.setOrgId(HomeContextSupport.currentOrgId());
            preference.setUserId(HomeContextSupport.currentUserId());
            preference.setDefaultHomePageId(homePageId);
            preference.setDefaultHomeRef(homeRef);
            preferenceMapper.insert(preference);
            return;
        }
        preference.setDefaultHomePageId(homePageId);
        preference.setDefaultHomeRef(homeRef);
        preference.setUpdatedBy(MangoContextHolder.userId());
        preferenceMapper.updateById(preference);
    }

    private LambdaQueryWrapper<UserHomePreferenceEntity> preferenceWrapper() {
        return new LambdaQueryWrapper<UserHomePreferenceEntity>()
                .eq(UserHomePreferenceEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(UserHomePreferenceEntity::getUserId, HomeContextSupport.currentUserId());
    }

    private UserHomePreferenceEntity preferenceForUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return preferenceMapper.selectOne(new LambdaQueryWrapper<UserHomePreferenceEntity>()
                .eq(UserHomePreferenceEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(UserHomePreferenceEntity::getUserId, userId));
    }

    private HomePageVO builtInDefault() {
        HomePageVO vo = new HomePageVO();
        vo.setRouteKey("__built_in__");
        vo.setName(BUILT_IN_NAME);
        vo.setLayoutJson(HomeLayoutSupport.defaultLayoutJson());
        vo.setSort(0);
        vo.setEnabled(true);
        vo.setDefaultPage(true);
        vo.setBuiltIn(true);
        vo.setSourceType(HomePageSourceType.SYSTEM.name());
        vo.setSourceLabel("系统默认");
        vo.setSourceLabels(List.of("系统默认"));
        vo.setReadOnly(true);
        vo.setCanCopy(true);
        return vo;
    }

    private HomePageVO toVO(UserHomePageEntity entity, boolean defaultPage) {
        HomePageVO vo = new HomePageVO();
        vo.setId(entity.getId());
        vo.setRouteKey(HomeRouteKeys.user(entity.getId()));
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setName(entity.getName());
        vo.setLayoutJson(entity.getLayoutJson());
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled());
        vo.setDefaultPage(defaultPage);
        vo.setBuiltIn(false);
        vo.setSourceType(HomePageSourceType.USER.name());
        vo.setSourceLabel("自建");
        vo.setSourceLabels(List.of("自建"));
        vo.setReadOnly(false);
        vo.setCanCopy(true);
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private HomePageVO toAuthorizedVO(HomeTemplateEntity template,
                                      HomeTemplateVersionEntity version,
                                      AuthorizedTemplateMatch match,
                                      boolean defaultPage) {
        HomePageVO vo = new HomePageVO();
        vo.setRouteKey(HomeRouteKeys.template(template.getId()));
        vo.setTenantId(template.getTenantId());
        vo.setTemplateId(template.getId());
        vo.setTemplateVersionId(version.getId());
        vo.setName(template.getName());
        vo.setLayoutJson(version.getLayoutJson());
        vo.setSort(template.getSort());
        vo.setEnabled(template.getEnabled());
        vo.setDefaultPage(defaultPage || Boolean.TRUE.equals(match.defaultFlag()));
        vo.setBuiltIn(false);
        vo.setSourceType(resolveSourceType(match.sourceLabels()));
        vo.setSourceLabels(List.copyOf(match.sourceLabels()));
        vo.setSourceLabel(String.join("，", match.sourceLabels()));
        vo.setReadOnly(true);
        vo.setCanCopy(true);
        vo.setCreatedAt(version.getCreatedAt());
        vo.setUpdatedAt(version.getUpdatedAt());
        return vo;
    }

    private String resolveSourceType(List<String> labels) {
        if (labels.stream().anyMatch(label -> label.startsWith("个人授权"))) {
            return HomePageSourceType.PERSONAL_AUTH.name();
        }
        if (labels.stream().anyMatch(label -> label.startsWith("部门授权"))) {
            return HomePageSourceType.ORG_AUTH.name();
        }
        if (labels.stream().anyMatch(label -> label.startsWith("角色授权"))) {
            return HomePageSourceType.ROLE_AUTH.name();
        }
        return HomePageSourceType.SYSTEM.name();
    }

    private record AuthorizedTemplateMatch(Long templateId, Boolean defaultFlag, List<String> sourceLabels) {
    }
}
