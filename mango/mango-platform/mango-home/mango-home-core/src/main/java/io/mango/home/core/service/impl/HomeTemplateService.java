package io.mango.home.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.home.api.command.CreateHomeTemplateCommand;
import io.mango.home.api.command.SaveHomeTemplateAuthorizationsCommand;
import io.mango.home.api.command.UpdateHomeTemplateDraftCommand;
import io.mango.home.api.command.UpdateHomeTemplateStatusCommand;
import io.mango.home.api.enums.HomePageSourceType;
import io.mango.home.api.enums.HomeTemplateAuthorizationSubjectType;
import io.mango.home.api.enums.HomeTemplateVersionStatus;
import io.mango.home.api.query.HomeTemplateAuthorizationQuery;
import io.mango.home.api.query.HomeTemplateQuery;
import io.mango.home.api.query.UserHomeViewQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.api.vo.HomeTemplateAuthorizationItem;
import io.mango.home.api.vo.HomeTemplateAuthorizationVO;
import io.mango.home.api.vo.HomeTemplateVO;
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
import io.mango.home.core.service.IHomeTemplateService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.entity.SysOrg;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomeTemplateService implements IHomeTemplateService {

    private static final int DEFAULT_SORT_STEP = 10;
    private static final int FIRST_DRAFT_VERSION = 0;

    private final HomeTemplateMapper templateMapper;
    private final HomeTemplateVersionMapper versionMapper;
    private final HomeTemplateAuthorizationMapper authorizationMapper;
    private final UserHomePageMapper homePageMapper;
    private final UserHomePreferenceMapper preferenceMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<IAuthorizationProvider> authorizationProvider;
    private final ObjectProvider<SysOrgApi> sysOrgApiProvider;

    @Override
    public List<HomeTemplateVO> list(HomeTemplateQuery query) {
        HomeTemplateQuery resolved = query == null ? new HomeTemplateQuery() : query;
        List<HomeTemplateEntity> templates = templateMapper.selectList(new LambdaQueryWrapper<HomeTemplateEntity>()
                .eq(HomeTemplateEntity::getTenantId, HomeContextSupport.currentTenantId())
                .like(StringUtils.hasText(resolved.getKeyword()), HomeTemplateEntity::getName, resolved.getKeyword())
                .eq(resolved.getEnabled() != null, HomeTemplateEntity::getEnabled, resolved.getEnabled())
                .orderByAsc(HomeTemplateEntity::getSort)
                .orderByDesc(HomeTemplateEntity::getUpdatedAt));
        return templates.stream().map(this::toVO).toList();
    }

    @Override
    public HomeTemplateVO detail(Long id) {
        return toVO(requiredTemplate(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeTemplateVO create(CreateHomeTemplateCommand command) {
        Require.notNull(command, "模板创建命令不能为空");
        Require.notBlank(command.getName(), "模板名称不能为空");
        String layoutJson = HomeLayoutSupport.normalize(objectMapper, command.getLayoutJson());
        HomeTemplateEntity template = new HomeTemplateEntity();
        template.setTenantId(HomeContextSupport.currentTenantId());
        template.setName(command.getName().trim());
        template.setEnabled(true);
        template.setActiveVersionNo(0);
        template.setSort(nextSort());
        templateMapper.insert(template);
        HomeTemplateVersionEntity draft = newDraftVersion(template.getId(), FIRST_DRAFT_VERSION, layoutJson, null);
        versionMapper.insert(draft);
        return toVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeTemplateVO updateDraft(UpdateHomeTemplateDraftCommand command) {
        Require.notNull(command, "模板草稿命令不能为空");
        Require.notBlank(command.getName(), "模板名称不能为空");
        Require.notBlank(command.getLayoutJson(), "模板布局不能为空");
        HomeLayoutSupport.validate(objectMapper, command.getLayoutJson());
        HomeTemplateEntity template = requiredTemplate(command.getId());
        HomeTemplateVersionEntity draft = draftVersion(template.getId());
        template.setName(command.getName().trim());
        template.setUpdatedBy(MangoContextHolder.userId());
        templateMapper.updateById(template);
        if (draft == null) {
            HomeTemplateVersionEntity active = activeVersion(template);
            Require.notNull(active, "模板没有可编辑版本");
            draft = newDraftVersion(template.getId(), nextDraftVersionNo(template), command.getLayoutJson(), active.getId());
            draft.setCreatedBy(MangoContextHolder.userId());
            draft.setUpdatedBy(MangoContextHolder.userId());
            versionMapper.insert(draft);
        } else {
            draft.setLayoutJson(command.getLayoutJson());
            draft.setUpdatedBy(MangoContextHolder.userId());
            versionMapper.updateById(draft);
        }
        return toVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeTemplateVO copy(Long id) {
        HomeTemplateEntity source = requiredTemplate(id);
        HomeTemplateVersionEntity sourceVersion = activeVersion(source);
        if (sourceVersion == null) {
            sourceVersion = draftVersion(source.getId());
        }
        Require.notNull(sourceVersion, "模板没有可复制版本");
        HomeTemplateEntity copy = new HomeTemplateEntity();
        copy.setTenantId(HomeContextSupport.currentTenantId());
        copy.setName(source.getName() + " 副本");
        copy.setEnabled(true);
        copy.setActiveVersionNo(0);
        copy.setSort(nextSort());
        templateMapper.insert(copy);
        HomeTemplateVersionEntity draft = newDraftVersion(copy.getId(), FIRST_DRAFT_VERSION,
                sourceVersion.getLayoutJson(), sourceVersion.getId());
        versionMapper.insert(draft);
        return toVO(copy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeTemplateVO publish(Long id) {
        HomeTemplateEntity template = requiredTemplate(id);
        HomeTemplateVersionEntity draft = draftVersion(template.getId());
        Require.notNull(draft, "模板没有可发布草稿");
        versionMapper.update(null, new LambdaUpdateWrapper<HomeTemplateVersionEntity>()
                .eq(HomeTemplateVersionEntity::getTemplateId, template.getId())
                .eq(HomeTemplateVersionEntity::getStatus, HomeTemplateVersionStatus.ACTIVE.name())
                .set(HomeTemplateVersionEntity::getStatus, HomeTemplateVersionStatus.HISTORY.name()));
        int nextVersionNo = template.getActiveVersionNo() == null ? 1 : template.getActiveVersionNo() + 1;
        draft.setVersionNo(nextVersionNo);
        draft.setStatus(HomeTemplateVersionStatus.ACTIVE.name());
        draft.setPublishedBy(MangoContextHolder.userId());
        draft.setPublishedAt(LocalDateTime.now());
        draft.setUpdatedBy(MangoContextHolder.userId());
        versionMapper.updateById(draft);
        template.setActiveVersionId(draft.getId());
        template.setActiveVersionNo(nextVersionNo);
        template.setUpdatedBy(MangoContextHolder.userId());
        templateMapper.updateById(template);
        return toVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeTemplateVO updateStatus(UpdateHomeTemplateStatusCommand command) {
        Require.notNull(command, "模板状态命令不能为空");
        Require.notNull(command.getEnabled(), "模板状态不能为空");
        HomeTemplateEntity template = requiredTemplate(command.getId());
        template.setEnabled(command.getEnabled());
        template.setUpdatedBy(MangoContextHolder.userId());
        templateMapper.updateById(template);
        return toVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HomeTemplateEntity template = requiredTemplate(id);
        Long authCount = authorizationMapper.selectCount(new LambdaQueryWrapper<HomeTemplateAuthorizationEntity>()
                .eq(HomeTemplateAuthorizationEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(HomeTemplateAuthorizationEntity::getTemplateId, template.getId())
                .eq(HomeTemplateAuthorizationEntity::getEnabled, true));
        Require.isTrue(authCount == null || authCount == 0, "已授权模板不能删除，请先撤销授权或停用模板");
        versionMapper.delete(new LambdaQueryWrapper<HomeTemplateVersionEntity>()
                .eq(HomeTemplateVersionEntity::getTemplateId, template.getId()));
        templateMapper.deleteById(template.getId());
    }

    @Override
    public List<HomeTemplateAuthorizationVO> listAuthorizations(HomeTemplateAuthorizationQuery query) {
        Require.notNull(query, "授权查询不能为空");
        requiredTemplate(query.getTemplateId());
        return authorizationMapper.selectList(new LambdaQueryWrapper<HomeTemplateAuthorizationEntity>()
                        .eq(HomeTemplateAuthorizationEntity::getTenantId, HomeContextSupport.currentTenantId())
                        .eq(HomeTemplateAuthorizationEntity::getTemplateId, query.getTemplateId())
                        .eq(HomeTemplateAuthorizationEntity::getEnabled, true)
                        .orderByAsc(HomeTemplateAuthorizationEntity::getSort)
                        .orderByAsc(HomeTemplateAuthorizationEntity::getCreatedAt))
                .stream().map(this::toAuthorizationVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<HomeTemplateAuthorizationVO> saveAuthorizations(SaveHomeTemplateAuthorizationsCommand command) {
        Require.notNull(command, "授权保存命令不能为空");
        requiredTemplate(command.getTemplateId());
        authorizationMapper.delete(new LambdaQueryWrapper<HomeTemplateAuthorizationEntity>()
                .eq(HomeTemplateAuthorizationEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(HomeTemplateAuthorizationEntity::getTemplateId, command.getTemplateId()));
        int sort = DEFAULT_SORT_STEP;
        Set<String> subjects = new LinkedHashSet<>();
        for (HomeTemplateAuthorizationItem item : command.getAuthorizations()) {
            validateAuthorizationItem(item);
            String subjectKey = authorizationSubjectKey(item);
            Require.isTrue(subjects.add(subjectKey), "授权对象不能重复");
            HomeTemplateAuthorizationEntity entity = new HomeTemplateAuthorizationEntity();
            entity.setTenantId(HomeContextSupport.currentTenantId());
            entity.setTemplateId(command.getTemplateId());
            entity.setSubjectType(item.getSubjectType().name());
            entity.setSubjectId(item.getSubjectId() == null ? 0L : item.getSubjectId());
            entity.setSubjectCode(defaultString(item.getSubjectCode()));
            entity.setSubjectName(trimToNull(item.getSubjectName()));
            entity.setDefaultFlag(Boolean.TRUE.equals(item.getDefaultFlag()));
            entity.setSort(item.getSort() == null ? sort : item.getSort());
            entity.setEnabled(true);
            authorizationMapper.insert(entity);
            sort += DEFAULT_SORT_STEP;
        }
        HomeTemplateAuthorizationQuery query = new HomeTemplateAuthorizationQuery();
        query.setTemplateId(command.getTemplateId());
        return listAuthorizations(query);
    }

    @Override
    public List<HomePageVO> resolveUserPages(UserHomeViewQuery query) {
        Require.notNull(query, "用户首页查询不能为空");
        Require.notNull(query.getUserId(), "用户ID不能为空");
        UserHomePreferenceEntity preference = preferenceMapper.selectOne(new LambdaQueryWrapper<UserHomePreferenceEntity>()
                .eq(UserHomePreferenceEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(UserHomePreferenceEntity::getUserId, query.getUserId()));
        List<HomePageVO> result = new ArrayList<>();
        for (UserHomePageEntity page : listUserPages(query.getUserId())) {
            result.add(toUserHomeVO(page, isDefaultRoute(HomeRouteKeys.user(page.getId()), preference)));
        }
        result.addAll(resolveAuthorizedPages(query, preference));
        if (result.isEmpty()) {
            result.add(builtInDefault());
        }
        applyDefaultSelection(result, preference);
        return result;
    }

    private void validateAuthorizationItem(HomeTemplateAuthorizationItem item) {
        Require.notNull(item, "授权项不能为空");
        Require.notNull(item.getSubjectType(), "授权对象类型不能为空");
        if (item.getSubjectType() == HomeTemplateAuthorizationSubjectType.ROLE) {
            Require.notBlank(item.getSubjectCode(), "角色授权必须填写角色编码");
            return;
        }
        Require.notNull(item.getSubjectId(), "个人或部门授权必须填写对象ID");
    }

    private String authorizationSubjectKey(HomeTemplateAuthorizationItem item) {
        if (item.getSubjectType() == HomeTemplateAuthorizationSubjectType.ROLE) {
            return item.getSubjectType().name() + ":" + item.getSubjectCode().trim();
        }
        return item.getSubjectType().name() + ":" + item.getSubjectId();
    }

    private List<UserHomePageEntity> listUserPages(Long userId) {
        return homePageMapper.selectList(new LambdaQueryWrapper<UserHomePageEntity>()
                .eq(UserHomePageEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(UserHomePageEntity::getUserId, userId)
                .eq(UserHomePageEntity::getEnabled, true)
                .orderByAsc(UserHomePageEntity::getSort)
                .orderByAsc(UserHomePageEntity::getCreatedAt)
                .orderByAsc(UserHomePageEntity::getId));
    }

    private List<HomePageVO> resolveAuthorizedPages(UserHomeViewQuery query, UserHomePreferenceEntity preference) {
        String tenantId = HomeContextSupport.currentTenantId();
        Map<Long, AuthorizedTemplateMatch> matches = new LinkedHashMap<>();
        addAuthorizationMatches(matches, listUserAuthorizations(tenantId, query.getUserId()));
        addAuthorizationMatches(matches, listOrgAuthorizations(tenantId, query.getOrgId()));
        addAuthorizationMatches(matches, listRoleAuthorizations(tenantId, query));
        List<HomePageVO> result = new ArrayList<>();
        for (AuthorizedTemplateMatch match : matches.values()) {
            HomeTemplateEntity template = templateMapper.selectOne(new LambdaQueryWrapper<HomeTemplateEntity>()
                    .eq(HomeTemplateEntity::getTenantId, tenantId)
                    .eq(HomeTemplateEntity::getId, match.templateId())
                    .eq(HomeTemplateEntity::getEnabled, true));
            if (template == null || template.getActiveVersionId() == null) {
                continue;
            }
            HomeTemplateVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<HomeTemplateVersionEntity>()
                    .eq(HomeTemplateVersionEntity::getTenantId, tenantId)
                    .eq(HomeTemplateVersionEntity::getId, template.getActiveVersionId())
                    .eq(HomeTemplateVersionEntity::getStatus, HomeTemplateVersionStatus.ACTIVE.name()));
            if (version == null) {
                continue;
            }
            result.add(toAuthorizedHomeVO(template, version, match,
                    isDefaultRoute(HomeRouteKeys.template(template.getId()), preference)));
        }
        result.sort(Comparator.comparing(HomePageVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(HomePageVO::getName, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private List<HomeTemplateAuthorizationEntity> listUserAuthorizations(String tenantId, Long userId) {
        return authorizationMapper.selectList(baseAuthorizationWrapper(tenantId)
                .eq(HomeTemplateAuthorizationEntity::getSubjectType, HomeTemplateAuthorizationSubjectType.USER.name())
                .eq(HomeTemplateAuthorizationEntity::getSubjectId, userId));
    }

    private List<HomeTemplateAuthorizationEntity> listOrgAuthorizations(String tenantId, Long orgId) {
        Set<Long> orgIds = orgAndAncestors(orgId);
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return authorizationMapper.selectList(baseAuthorizationWrapper(tenantId)
                .eq(HomeTemplateAuthorizationEntity::getSubjectType, HomeTemplateAuthorizationSubjectType.ORG.name())
                .in(HomeTemplateAuthorizationEntity::getSubjectId, orgIds));
    }

    private List<HomeTemplateAuthorizationEntity> listRoleAuthorizations(String tenantId, UserHomeViewQuery query) {
        Set<String> roleCodes = roleCodes(query);
        if (roleCodes.isEmpty()) {
            return List.of();
        }
        return authorizationMapper.selectList(baseAuthorizationWrapper(tenantId)
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

    private Set<Long> orgAndAncestors(Long orgId) {
        LinkedHashSet<Long> orgIds = new LinkedHashSet<>();
        if (orgId == null) {
            return orgIds;
        }
        orgIds.add(orgId);
        SysOrgApi sysOrgApi = sysOrgApiProvider.getIfAvailable();
        if (sysOrgApi == null) {
            return orgIds;
        }
        Long cursor = orgId;
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

    private Set<String> roleCodes(UserHomeViewQuery query) {
        IAuthorizationProvider provider = authorizationProvider.getIfAvailable();
        if (provider == null || query.getMemberId() == null) {
            return Set.of();
        }
        AuthorizationQuery authorizationQuery = AuthorizationQuery.member(query.getMemberId())
                .withTenantId(MangoContextHolder.tenantId())
                .withSystemCode(MangoContextHolder.appCode())
                .withRealm(MangoContextHolder.get().realm())
                .withActorType(MangoContextHolder.get().actorType())
                .withParty(MangoContextHolder.get().partyType(), query.getOrgId());
        AuthorizationSnapshot snapshot = provider.load(authorizationQuery);
        return snapshot == null ? Set.of() : snapshot.roleCodes();
    }

    private HomeTemplateEntity requiredTemplate(Long id) {
        Require.notNull(id, "模板ID不能为空");
        HomeTemplateEntity template = templateMapper.selectOne(new LambdaQueryWrapper<HomeTemplateEntity>()
                .eq(HomeTemplateEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(HomeTemplateEntity::getId, id));
        Require.notNull(template, "首页模板不存在");
        return template;
    }

    private HomeTemplateVersionEntity draftVersion(Long templateId) {
        return versionMapper.selectOne(new LambdaQueryWrapper<HomeTemplateVersionEntity>()
                .eq(HomeTemplateVersionEntity::getTenantId, HomeContextSupport.currentTenantId())
                .eq(HomeTemplateVersionEntity::getTemplateId, templateId)
                .eq(HomeTemplateVersionEntity::getStatus, HomeTemplateVersionStatus.DRAFT.name())
                .orderByDesc(HomeTemplateVersionEntity::getCreatedAt)
                .last("LIMIT 1"));
    }

    private HomeTemplateVersionEntity activeVersion(HomeTemplateEntity template) {
        if (template.getActiveVersionId() == null) {
            return null;
        }
        return versionMapper.selectById(template.getActiveVersionId());
    }

    private HomeTemplateVersionEntity newDraftVersion(Long templateId, Integer versionNo, String layoutJson, Long sourceVersionId) {
        HomeTemplateVersionEntity draft = new HomeTemplateVersionEntity();
        draft.setTenantId(HomeContextSupport.currentTenantId());
        draft.setTemplateId(templateId);
        draft.setVersionNo(versionNo);
        draft.setStatus(HomeTemplateVersionStatus.DRAFT.name());
        draft.setLayoutJson(layoutJson);
        draft.setSourceVersionId(sourceVersionId);
        return draft;
    }

    private int nextDraftVersionNo(HomeTemplateEntity template) {
        return template.getActiveVersionNo() == null ? FIRST_DRAFT_VERSION : template.getActiveVersionNo() + 1;
    }

    private int nextSort() {
        List<HomeTemplateEntity> templates = templateMapper.selectList(new LambdaQueryWrapper<HomeTemplateEntity>()
                .eq(HomeTemplateEntity::getTenantId, HomeContextSupport.currentTenantId())
                .orderByAsc(HomeTemplateEntity::getSort));
        if (templates.isEmpty()) {
            return DEFAULT_SORT_STEP;
        }
        return templates.get(templates.size() - 1).getSort() + DEFAULT_SORT_STEP;
    }

    private HomeTemplateVO toVO(HomeTemplateEntity entity) {
        HomeTemplateVO vo = new HomeTemplateVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setName(entity.getName());
        vo.setEnabled(entity.getEnabled());
        vo.setActiveVersionId(entity.getActiveVersionId());
        vo.setActiveVersionNo(entity.getActiveVersionNo());
        HomeTemplateVersionEntity active = activeVersion(entity);
        vo.setActiveLayoutJson(active == null ? null : active.getLayoutJson());
        HomeTemplateVersionEntity draft = draftVersion(entity.getId());
        vo.setDraftVersionId(draft == null ? null : draft.getId());
        vo.setDraftLayoutJson(draft == null ? null : draft.getLayoutJson());
        Long authCount = authorizationMapper.selectCount(new LambdaQueryWrapper<HomeTemplateAuthorizationEntity>()
                .eq(HomeTemplateAuthorizationEntity::getTenantId, entity.getTenantId())
                .eq(HomeTemplateAuthorizationEntity::getTemplateId, entity.getId())
                .eq(HomeTemplateAuthorizationEntity::getEnabled, true));
        vo.setAuthorizationCount(authCount == null ? 0 : authCount.intValue());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private HomeTemplateAuthorizationVO toAuthorizationVO(HomeTemplateAuthorizationEntity entity) {
        HomeTemplateAuthorizationVO vo = new HomeTemplateAuthorizationVO();
        vo.setId(entity.getId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setSubjectType(HomeTemplateAuthorizationSubjectType.valueOf(entity.getSubjectType()));
        vo.setSubjectId(entity.getSubjectId());
        vo.setSubjectCode(entity.getSubjectCode());
        vo.setSubjectName(entity.getSubjectName());
        vo.setDefaultFlag(entity.getDefaultFlag());
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private HomePageVO toUserHomeVO(UserHomePageEntity entity, boolean defaultPage) {
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

    private HomePageVO toAuthorizedHomeVO(HomeTemplateEntity template,
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

    private HomePageVO builtInDefault() {
        HomePageVO vo = new HomePageVO();
        vo.setRouteKey("__built_in__");
        vo.setName("系统工作台");
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
        HomePageVO selected = firstAuthorizationDefault(pages, authorizationDefaultRoutes, HomePageSourceType.PERSONAL_AUTH);
        if (selected == null) {
            selected = firstAuthorizationDefault(pages, authorizationDefaultRoutes, HomePageSourceType.ORG_AUTH);
        }
        if (selected == null) {
            selected = firstAuthorizationDefault(pages, authorizationDefaultRoutes, HomePageSourceType.ROLE_AUTH);
        }
        if (selected == null) {
            selected = pages.get(0);
        }
        selected.setDefaultPage(true);
    }

    private HomePageVO firstAuthorizationDefault(List<HomePageVO> pages, Set<String> defaultRoutes, HomePageSourceType sourceType) {
        return pages.stream()
                .filter(page -> sourceType.name().equals(page.getSourceType()))
                .filter(page -> defaultRoutes.contains(page.getRouteKey()))
                .findFirst()
                .orElse(null);
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

    private String sourceLabel(HomeTemplateAuthorizationEntity authorization) {
        String name = authorization.getSubjectName();
        if (!StringUtils.hasText(name)) {
            name = StringUtils.hasText(authorization.getSubjectCode())
                    ? authorization.getSubjectCode()
                    : String.valueOf(authorization.getSubjectId());
        }
        return switch (HomeTemplateAuthorizationSubjectType.valueOf(authorization.getSubjectType())) {
            case USER -> "个人授权：" + name;
            case ORG -> "部门授权：" + name;
            case ROLE -> "角色授权：" + name;
        };
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

    private String defaultString(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record AuthorizedTemplateMatch(Long templateId, Boolean defaultFlag, List<String> sourceLabels) {
    }
}
