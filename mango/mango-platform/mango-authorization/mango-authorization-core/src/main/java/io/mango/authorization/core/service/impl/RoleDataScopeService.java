package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.SaveRoleDataScopeCommand;
import io.mango.authorization.api.enums.DataScopeMode;
import io.mango.authorization.api.query.EffectiveDataScopeQuery;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.api.vo.RoleDataScopeVO;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.RoleDataScopeEntity;
import io.mango.authorization.core.entity.RoleMenuEntity;
import io.mango.authorization.core.entity.SubjectRoleBindingEntity;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleDataScopeMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.IRoleDataScopeService;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色数据权限服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleDataScopeService implements IRoleDataScopeService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final RoleDataScopeMapper roleDataScopeMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<RoleDataScopeVO> listByRole(Long roleId) {
        RoleEntity role = roleMapper.selectById(roleId);
        boolean currentTenantRole = isCurrentTenantRole(role);
        if (!currentTenantRole) {
            log.warn("Tenant isolation violation: attempt to query data scopes of role {} by tenant {}",
                    roleId, getTenantIdLong());
        }
        Require.isTrue(currentTenantRole, AuthorizationCode.AUTHORIZATION_FORBIDDEN,
                "无权查询该角色的数据权限");
        LambdaQueryWrapper<RoleDataScopeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDataScopeEntity::getRoleId, roleId)
                .eq(RoleDataScopeEntity::getTenantId, role.getTenantId())
                .eq(RoleDataScopeEntity::getAppCode, role.getAppCode())
                .orderByAsc(RoleDataScopeEntity::getResourceCode);
        return roleDataScopeMapper.selectList(wrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean save(SaveRoleDataScopeCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "数据权限保存命令不能为空");
        RoleEntity role = roleMapper.selectById(command.getRoleId());
        boolean currentTenantRole = isCurrentTenantRole(role);
        if (!currentTenantRole) {
            log.warn("Tenant isolation violation: attempt to save data scope for role {} by tenant {}",
                    command.getRoleId(), getTenantIdLong());
        }
        Require.isTrue(currentTenantRole, AuthorizationCode.AUTHORIZATION_FORBIDDEN,
                "无权配置该角色的数据权限");

        String resourceCode = normalizeRequired(command.getResourceCode(), "资源编码不能为空");
        boolean grantedQueryResource = isRoleGrantedQueryResource(role, resourceCode);
        if (!grantedQueryResource) {
            log.warn("Data scope resource escalation denied: roleId={}, tenantId={}, resourceCode={}",
                    role.getRoleId(), getTenantIdLong(), resourceCode);
            return false;
        }
        DataScopeMode mode = command.getScopeMode();
        Require.notNull(mode, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "数据范围模式不能为空");
        List<String> values = normalizeScopeValues(command.getScopeValues());

        LambdaQueryWrapper<RoleDataScopeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDataScopeEntity::getTenantId, role.getTenantId())
                .eq(RoleDataScopeEntity::getAppCode, role.getAppCode())
                .eq(RoleDataScopeEntity::getRoleId, role.getRoleId())
                .eq(RoleDataScopeEntity::getResourceCode, resourceCode);
        RoleDataScopeEntity entity = roleDataScopeMapper.selectOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            entity = new RoleDataScopeEntity();
            entity.setTenantId(role.getTenantId());
            entity.setAppCode(role.getAppCode());
            entity.setRoleId(role.getRoleId());
            entity.setResourceCode(resourceCode);
            entity.setCreateTime(now);
        }
        entity.setScopeMode(mode.name());
        entity.setScopeValues(writeScopeValues(values));
        entity.setIncludeChildren(Boolean.TRUE.equals(command.getIncludeChildren()));
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        entity.setUpdateTime(now);
        if (entity.getId() == null) {
            return roleDataScopeMapper.insert(entity) > 0;
        }
        return roleDataScopeMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    public Boolean delete(Long roleId, String resourceCode) {
        Require.notNull(roleId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "角色ID不能为空");
        RoleEntity role = roleMapper.selectById(roleId);
        boolean currentTenantRole = isCurrentTenantRole(role);
        if (!currentTenantRole) {
            log.warn("Tenant isolation violation: attempt to delete data scope for role {} by tenant {}",
                    roleId, getTenantIdLong());
        }
        Require.isTrue(currentTenantRole, AuthorizationCode.AUTHORIZATION_FORBIDDEN,
                "无权删除该角色的数据权限");
        LambdaQueryWrapper<RoleDataScopeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDataScopeEntity::getTenantId, role.getTenantId())
                .eq(RoleDataScopeEntity::getAppCode, role.getAppCode())
                .eq(RoleDataScopeEntity::getRoleId, roleId)
                .eq(RoleDataScopeEntity::getResourceCode, normalizeRequired(resourceCode, "资源编码不能为空"));
        roleDataScopeMapper.delete(wrapper);
        return true;
    }

    @Override
    public EffectiveDataScopeVO resolveCurrent(EffectiveDataScopeQuery query) {
        Require.notNull(query, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "数据权限查询条件不能为空");
        MangoContextSnapshot context = MangoContextHolder.get();
        Require.notNull(context.memberId(), AuthorizationCode.AUTHORIZATION_UNAUTHORIZED,
                "缺少登录成员上下文");
        AuthorizationQuery authorizationQuery = AuthorizationQuery.member(context.memberId())
                .withTenantId(context.tenantId())
                .withSystemCode(StringUtils.hasText(query.getAppCode()) ? query.getAppCode() : context.appCode())
                .withRealm(context.realm())
                .withActorType(context.actorType())
                .withParty(context.partyType(), context.partyId());
        return resolve(authorizationQuery, query.getResourceCode());
    }

    @Override
    public EffectiveDataScopeVO resolve(AuthorizationQuery query, String resourceCode) {
        String normalizedResourceCode = normalizeRequired(resourceCode, "资源编码不能为空");
        List<Long> roleIds = listSubjectRoleIds(query);
        EffectiveDataScopeVO result = new EffectiveDataScopeVO();
        result.setResourceCode(normalizedResourceCode);
        if (roleIds.isEmpty()) {
            result.setScopeMode(DataScopeMode.SELF);
            result.setSelfIncluded(true);
            return result;
        }

        LambdaQueryWrapper<RoleDataScopeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RoleDataScopeEntity::getRoleId, roleIds)
                .eq(RoleDataScopeEntity::getResourceCode, normalizedResourceCode)
                .eq(RoleDataScopeEntity::getStatus, 1);
        Long tenantId = parseTenantId(query.tenantId());
        wrapper.eq(tenantId != null, RoleDataScopeEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), RoleDataScopeEntity::getAppCode, query.systemCode());
        List<RoleDataScopeEntity> scopes = roleDataScopeMapper.selectList(wrapper);
        if (scopes.isEmpty()) {
            result.setScopeMode(DataScopeMode.SELF);
            result.setSelfIncluded(true);
            return result;
        }

        boolean selfIncluded = false;
        boolean orgConfigured = false;
        boolean selfOrgConfigured = false;
        boolean selfOrgChildrenConfigured = false;
        Set<String> orgValues = new LinkedHashSet<>();
        for (RoleDataScopeEntity scope : scopes) {
            DataScopeMode mode = parseMode(scope.getScopeMode());
            if (mode == DataScopeMode.ALL) {
                result.setScopeMode(DataScopeMode.ALL);
                result.setScopeValues(Collections.emptyList());
                result.setSelfIncluded(false);
                return result;
            }
            if (mode == DataScopeMode.SELF) {
                selfIncluded = true;
                continue;
            }
            if (mode == DataScopeMode.SELF_ORG) {
                selfOrgConfigured = true;
                continue;
            }
            if (mode == DataScopeMode.SELF_ORG_AND_CHILDREN) {
                selfOrgChildrenConfigured = true;
                continue;
            }
            if (mode == DataScopeMode.ORG) {
                orgConfigured = true;
                orgValues.addAll(readScopeValues(scope.getScopeValues()));
            }
        }

        if (orgConfigured || selfOrgConfigured || selfOrgChildrenConfigured) {
            result.setScopeMode(resolveOrgMode(selfOrgConfigured, selfOrgChildrenConfigured, orgConfigured));
            result.setScopeValues(new ArrayList<>(orgValues));
            result.setSelfIncluded(selfIncluded);
            result.setIncludeChildren(selfOrgChildrenConfigured);
            return result;
        }
        result.setScopeMode(DataScopeMode.SELF);
        result.setSelfIncluded(true);
        return result;
    }

    private List<Long> listSubjectRoleIds(AuthorizationQuery query) {
        Long tenantId = parseTenantId(query.tenantId());
        if (StringUtils.hasText(query.tenantId()) && tenantId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<SubjectRoleBindingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectRoleBindingEntity::getSubjectId, query.subjectId())
                .eq(SubjectRoleBindingEntity::getSubjectType, query.subjectType())
                .eq(tenantId != null, SubjectRoleBindingEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), SubjectRoleBindingEntity::getAppCode, query.systemCode())
                .eq(StringUtils.hasText(query.realm()), SubjectRoleBindingEntity::getRealm, query.realm())
                .eq(StringUtils.hasText(query.actorType()), SubjectRoleBindingEntity::getActorType, query.actorType())
                .eq(StringUtils.hasText(query.partyType()), SubjectRoleBindingEntity::getPartyType, query.partyType())
                .eq(query.partyId() != null, SubjectRoleBindingEntity::getPartyId, query.partyId());
        return subjectRoleBindingMapper.selectList(wrapper)
                .stream()
                .map(SubjectRoleBindingEntity::getRoleId)
                .collect(Collectors.toList());
    }

    private RoleDataScopeVO toVO(RoleDataScopeEntity entity) {
        RoleDataScopeVO vo = new RoleDataScopeVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantIdAsLong());
        vo.setAppCode(entity.getAppCode());
        vo.setRoleId(entity.getRoleId());
        vo.setResourceCode(entity.getResourceCode());
        vo.setScopeMode(parseMode(entity.getScopeMode()));
        vo.setScopeValues(readScopeValues(entity.getScopeValues()));
        vo.setIncludeChildren(Boolean.TRUE.equals(entity.getIncludeChildren()));
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private DataScopeMode parseMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return DataScopeMode.SELF;
        }
        try {
            return DataScopeMode.valueOf(mode.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown data scope mode {}, fallback to SELF", mode);
            return DataScopeMode.SELF;
        }
    }

    private DataScopeMode resolveOrgMode(boolean selfOrgConfigured,
                                         boolean selfOrgChildrenConfigured,
                                         boolean orgConfigured) {
        if (selfOrgChildrenConfigured) {
            return DataScopeMode.SELF_ORG_AND_CHILDREN;
        }
        if (selfOrgConfigured) {
            return DataScopeMode.SELF_ORG;
        }
        if (orgConfigured) {
            return DataScopeMode.ORG;
        }
        return DataScopeMode.SELF;
    }

    private List<String> normalizeScopeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> readScopeValues(String valuesJson) {
        if (!StringUtils.hasText(valuesJson)) {
            return new ArrayList<>();
        }
        try {
            return normalizeScopeValues(objectMapper.readValue(valuesJson, STRING_LIST_TYPE));
        } catch (JsonProcessingException ex) {
            log.warn("Read role data scope values failed, fallback to empty values", ex);
            return new ArrayList<>();
        }
    }

    private boolean isRoleGrantedQueryResource(RoleEntity role, String resourceCode) {
        if (!isListResourceCode(resourceCode)) {
            return false;
        }
        LambdaQueryWrapper<RoleMenuEntity> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.eq(RoleMenuEntity::getRoleId, role.getRoleId())
                .eq(role.getTenantId() != null, RoleMenuEntity::getTenantId, role.getTenantId());
        List<Long> roleMenuIds = roleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(RoleMenuEntity::getMenuId)
                .toList();
        if (roleMenuIds.isEmpty()) {
            return false;
        }

        LambdaQueryWrapper<MenuEntity> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(MenuEntity::getId, roleMenuIds)
                .eq(MenuEntity::getAppCode, role.getAppCode())
                .eq(MenuEntity::getStatus, 1);
        return menuMapper.selectList(menuWrapper)
                .stream()
                .flatMap(menu -> splitPermissions(menu.getApiCodes()).stream())
                .anyMatch(resourceCode::equals);
    }

    private List<String> splitPermissions(String permissions) {
        if (!StringUtils.hasText(permissions)) {
            return Collections.emptyList();
        }
        return java.util.Arrays.stream(permissions.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private boolean isListResourceCode(String resourceCode) {
        return resourceCode.endsWith(":list");
    }

    private String writeScopeValues(List<String> values) {
        try {
            return objectMapper.writeValueAsString(normalizeScopeValues(values));
        } catch (JsonProcessingException ex) {
            return Require.fail(AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                    "序列化角色数据范围失败", ex);
        }
    }

    private boolean isCurrentTenantRole(RoleEntity role) {
        if (role == null) {
            return false;
        }
        Long currentTenantId = getTenantIdLong();
        return currentTenantId == null || currentTenantId.equals(role.getTenantIdAsLong());
    }

    private String normalizeRequired(String value, String message) {
        Require.notBlank(value, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, message);
        return value.trim();
    }

    private Long getTenantIdLong() {
        return parseTenantId(MangoContextHolder.tenantId());
    }

    private Long parseTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
