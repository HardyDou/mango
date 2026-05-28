package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.system.api.command.SavePersonalConfigCommand;
import io.mango.system.api.query.PersonalConfigQuery;
import io.mango.system.api.vo.PersonalConfigVO;
import io.mango.system.core.entity.SysPersonalConfigEntity;
import io.mango.system.core.mapper.SysPersonalConfigMapper;
import io.mango.system.core.service.IPersonalConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalConfigService implements IPersonalConfigService {

    private static final String DEFAULT_VALUE_TYPE = "JSON";

    private final SysPersonalConfigMapper personalConfigMapper;

    @Override
    public List<PersonalConfigVO> listCurrentUser(PersonalConfigQuery query) {
        Long userId = currentUserId();
        String tenantId = currentTenantId();
        LambdaQueryWrapper<SysPersonalConfigEntity> wrapper = userWrapper(tenantId, userId);
        if (query != null) {
            wrapper.eq(hasText(query.getGroupCode()), SysPersonalConfigEntity::getGroupCode, query.getGroupCode());
            wrapper.eq(hasText(query.getBizType()), SysPersonalConfigEntity::getBizType, query.getBizType());
            wrapper.eq(hasText(query.getConfigKey()), SysPersonalConfigEntity::getConfigKey, query.getConfigKey());
        }
        wrapper.orderByAsc(SysPersonalConfigEntity::getGroupCode)
                .orderByAsc(SysPersonalConfigEntity::getBizType)
                .orderByAsc(SysPersonalConfigEntity::getConfigKey);
        return personalConfigMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public PersonalConfigVO getCurrentUserValue(PersonalConfigQuery query) {
        Require.notNull(query, "查询条件不能为空");
        Require.notBlank(query.getGroupCode(), "groupCode不能为空");
        Require.notBlank(query.getBizType(), "bizType不能为空");
        Require.notBlank(query.getConfigKey(), "configKey不能为空");
        SysPersonalConfigEntity entity = personalConfigMapper.selectOne(configWrapper(
                currentTenantId(), currentUserId(), query.getGroupCode(), query.getBizType(), query.getConfigKey()));
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersonalConfigVO saveCurrentUser(SavePersonalConfigCommand command) {
        Require.notNull(command, "保存命令不能为空");
        Require.notBlank(command.getGroupCode(), "groupCode不能为空");
        Require.notBlank(command.getBizType(), "bizType不能为空");
        Require.notBlank(command.getConfigKey(), "configKey不能为空");
        Require.notBlank(command.getConfigValue(), "configValue不能为空");
        Long userId = currentUserId();
        String tenantId = currentTenantId();
        SysPersonalConfigEntity entity = personalConfigMapper.selectOne(configWrapper(
                tenantId, userId, command.getGroupCode(), command.getBizType(), command.getConfigKey()));
        if (entity == null) {
            entity = new SysPersonalConfigEntity();
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            entity.setGroupCode(command.getGroupCode());
            entity.setBizType(command.getBizType());
            entity.setConfigKey(command.getConfigKey());
            fillMutableFields(entity, command);
            personalConfigMapper.insert(entity);
        } else {
            fillMutableFields(entity, command);
            personalConfigMapper.updateById(entity);
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCurrentUser(PersonalConfigQuery query) {
        Require.notNull(query, "删除条件不能为空");
        Require.notBlank(query.getGroupCode(), "groupCode不能为空");
        Require.notBlank(query.getBizType(), "bizType不能为空");
        Require.notBlank(query.getConfigKey(), "configKey不能为空");
        return personalConfigMapper.delete(configWrapper(
                currentTenantId(), currentUserId(), query.getGroupCode(), query.getBizType(), query.getConfigKey())) > 0;
    }

    private void fillMutableFields(SysPersonalConfigEntity entity, SavePersonalConfigCommand command) {
        entity.setConfigValue(command.getConfigValue());
        entity.setValueType(hasText(command.getValueType()) ? command.getValueType() : DEFAULT_VALUE_TYPE);
        entity.setConfigName(command.getConfigName());
        entity.setRemark(command.getRemark());
        entity.setUpdatedBy(MangoContextHolder.userId());
    }

    private LambdaQueryWrapper<SysPersonalConfigEntity> userWrapper(String tenantId, Long userId) {
        return new LambdaQueryWrapper<SysPersonalConfigEntity>()
                .eq(SysPersonalConfigEntity::getTenantId, tenantId)
                .eq(SysPersonalConfigEntity::getUserId, userId);
    }

    private LambdaQueryWrapper<SysPersonalConfigEntity> configWrapper(
            String tenantId, Long userId, String groupCode, String bizType, String configKey) {
        return userWrapper(tenantId, userId)
                .eq(SysPersonalConfigEntity::getGroupCode, groupCode)
                .eq(SysPersonalConfigEntity::getBizType, bizType)
                .eq(SysPersonalConfigEntity::getConfigKey, configKey);
    }

    private PersonalConfigVO toVO(SysPersonalConfigEntity entity) {
        PersonalConfigVO vo = new PersonalConfigVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setGroupCode(entity.getGroupCode());
        vo.setBizType(entity.getBizType());
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigValue(entity.getConfigValue());
        vo.setValueType(entity.getValueType());
        vo.setConfigName(entity.getConfigName());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Long currentUserId() {
        Long userId = MangoContextHolder.userId();
        Require.notNull(userId, "缺少当前用户上下文");
        return userId;
    }

    private String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前租户上下文");
        return tenantId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
