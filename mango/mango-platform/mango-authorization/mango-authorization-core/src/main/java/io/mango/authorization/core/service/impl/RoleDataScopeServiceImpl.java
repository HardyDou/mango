package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.SaveRoleDataScopeCommand;
import io.mango.authorization.api.enums.DataScopeMode;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.api.vo.RoleDataScopeVO;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleDataScope;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleDataScopeMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.IRoleDataScopeService;
import io.mango.infra.context.core.MangoContextHolder;
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
public class RoleDataScopeServiceImpl implements IRoleDataScopeService {

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
        Role role = roleMapper.selectById(roleId);
        if (!isCurrentTenantRole(role)) {
            log.warn("Tenant isolation violation: attempt to query data scopes of role {} by tenant {}",
                    roleId, getTenantIdLong());
            return new ArrayList<>();
        }
        LambdaQueryWrapper<RoleDataScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDataScope::getRoleId, roleId)
                .eq(RoleDataScope::getTenantId, role.getTenantId())
                .eq(RoleDataScope::getAppCode, role.getAppCode())
                .orderByAsc(RoleDataScope::getResourceCode);
        return roleDataScopeMapper.selectList(wrapper)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean save(SaveRoleDataScopeCommand command) {
        Role role = roleMapper.selectById(command.getRoleId());
        if (!isCurrentTenantRole(role)) {
            log.warn("Tenant isolation violation: attempt to save data scope for role {} by tenant {}",
                    command.getRoleId(), getTenantIdLong());
            return false;
        }

        String resourceCode = normalizeRequired(command.getResourceCode(), "资源编码不能为空");
        if (!isRoleGrantedQueryResource(role, resourceCode)) {
            log.warn("Data scope resource escalation denied: roleId={}, tenantId={}, resourceCode={}",
                    role.getRoleId(), getTenantIdLong(), resourceCode);
            return false;
        }
        DataScopeMode mode = command.getScopeMode();
        if (mode == null) {
            throw new IllegalArgumentException("数据范围模式不能为空");
        }
        List<String> values = normalizeScopeValues(command.getScopeValues());

        LambdaQueryWrapper<RoleDataScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDataScope::getTenantId, role.getTenantId())
                .eq(RoleDataScope::getAppCode, role.getAppCode())
                .eq(RoleDataScope::getRoleId, role.getRoleId())
                .eq(RoleDataScope::getResourceCode, resourceCode);
        RoleDataScope entity = roleDataScopeMapper.selectOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            entity = new RoleDataScope();
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
        Role role = roleMapper.selectById(roleId);
        if (!isCurrentTenantRole(role)) {
            log.warn("Tenant isolation violation: attempt to delete data scope for role {} by tenant {}",
                    roleId, getTenantIdLong());
            return false;
        }
        LambdaQueryWrapper<RoleDataScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDataScope::getTenantId, role.getTenantId())
                .eq(RoleDataScope::getAppCode, role.getAppCode())
                .eq(RoleDataScope::getRoleId, roleId)
                .eq(RoleDataScope::getResourceCode, normalizeRequired(resourceCode, "资源编码不能为空"));
        roleDataScopeMapper.delete(wrapper);
        return true;
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

        LambdaQueryWrapper<RoleDataScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RoleDataScope::getRoleId, roleIds)
                .eq(RoleDataScope::getResourceCode, normalizedResourceCode)
                .eq(RoleDataScope::getStatus, 1);
        Long tenantId = parseTenantId(query.tenantId());
        wrapper.eq(tenantId != null, RoleDataScope::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), RoleDataScope::getAppCode, query.systemCode());
        List<RoleDataScope> scopes = roleDataScopeMapper.selectList(wrapper);
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
        for (RoleDataScope scope : scopes) {
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
        LambdaQueryWrapper<SubjectRoleBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectRoleBinding::getSubjectId, query.subjectId())
                .eq(SubjectRoleBinding::getSubjectType, query.subjectType())
                .eq(tenantId != null, SubjectRoleBinding::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), SubjectRoleBinding::getAppCode, query.systemCode())
                .eq(StringUtils.hasText(query.realm()), SubjectRoleBinding::getRealm, query.realm())
                .eq(StringUtils.hasText(query.actorType()), SubjectRoleBinding::getActorType, query.actorType())
                .eq(StringUtils.hasText(query.partyType()), SubjectRoleBinding::getPartyType, query.partyType())
                .eq(query.partyId() != null, SubjectRoleBinding::getPartyId, query.partyId());
        return subjectRoleBindingMapper.selectList(wrapper)
                .stream()
                .map(SubjectRoleBinding::getRoleId)
                .collect(Collectors.toList());
    }

    private RoleDataScopeVO toVO(RoleDataScope entity) {
        RoleDataScopeVO vo = new RoleDataScopeVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
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

    private boolean isRoleGrantedQueryResource(Role role, String resourceCode) {
        if (!isListResourceCode(resourceCode)) {
            return false;
        }
        LambdaQueryWrapper<RoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.eq(RoleMenu::getRoleId, role.getRoleId())
                .eq(role.getTenantId() != null, RoleMenu::getTenantId, role.getTenantId());
        List<Long> roleMenuIds = roleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(RoleMenu::getMenuId)
                .toList();
        if (roleMenuIds.isEmpty()) {
            return false;
        }

        LambdaQueryWrapper<Menu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(Menu::getMenuId, roleMenuIds)
                .eq(Menu::getAppCode, role.getAppCode())
                .eq(Menu::getStatus, 1);
        return menuMapper.selectList(menuWrapper)
                .stream()
                .flatMap(menu -> splitPermissions(menu.getPermissions()).stream())
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
            throw new IllegalStateException("Serialize role data scope values failed", ex);
        }
    }

    private boolean isCurrentTenantRole(Role role) {
        if (role == null) {
            return false;
        }
        Long currentTenantId = getTenantIdLong();
        return currentTenantId == null || currentTenantId.equals(role.getTenantId());
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
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
