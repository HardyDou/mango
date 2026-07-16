package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.system.api.enums.SystemCode;
import io.mango.system.api.command.SaveSysConfigCommand;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.mango.system.api.vo.SysConfigVO;
import io.mango.system.core.entity.SysConfigEntity;
import io.mango.system.core.mapper.SysConfigMapper;
import io.mango.system.core.service.ISysConfigService;
import io.mango.system.api.spi.SystemConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SysConfigService implements ISysConfigService, SystemConfigProvider {

    private static final int DISABLED = 0;

    private final SysConfigMapper sysConfigMapper;

    @Override
    public List<SysConfigVO> list(String type, String domainCode) {
        ConfigTypeEnum configType = null;
        if (StringUtils.hasText(type)) {
            try {
                configType = ConfigTypeEnum.valueOf(type.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return Require.fail(SystemCode.SYSTEM_INVALID, "配置类型不合法: " + type, exception);
            }
        }
        return query(configType, domainCode);
    }

    @Override
    public List<SysConfigVO> listByType(ConfigTypeEnum type, String domainCode) {
        Require.notNull(type, SystemCode.SYSTEM_INVALID, "配置类型不能为空");
        return query(type, domainCode);
    }

    private List<SysConfigVO> query(ConfigTypeEnum type, String domainCode) {
        LambdaQueryWrapper<SysConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, SysConfigEntity::getType, type)
                .eq(StringUtils.hasText(domainCode), SysConfigEntity::getDomainCode, trim(domainCode))
                .orderByAsc(SysConfigEntity::getSort);
        return sysConfigMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public SysConfigVO get(Long id) {
        return toVO(requireConfig(id));
    }

    @Override
    public Long create(SaveSysConfigCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        SysConfigEntity entity = new SysConfigEntity();
        copy(command, entity);
        sysConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean update(SaveSysConfigCommand command) {
        Require.notNull(command, SystemCode.SYSTEM_INVALID);
        Require.notNull(command.getId(), SystemCode.SYSTEM_INVALID, "ID不能为空");
        requireConfig(command.getId());
        SysConfigEntity entity = new SysConfigEntity();
        entity.setId(command.getId());
        copy(command, entity);
        return sysConfigMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean delete(Long id) {
        Require.notNull(id, SystemCode.SYSTEM_INVALID, "配置 ID 不能为空");
        requireConfig(id);
        return sysConfigMapper.deleteById(id) > 0;
    }

    @Override
    public Boolean updateValue(Long id, String value) {
        SysConfigEntity existing = requireConfig(id);
        Require.isTrue(!Integer.valueOf(DISABLED).equals(existing.getStatus()), SystemCode.CONFIG_NOT_EDITABLE, "配置已禁用");
        Require.isTrue(!Boolean.FALSE.equals(existing.getEditable()), SystemCode.CONFIG_NOT_EDITABLE,
                StringUtils.hasText(existing.getEditableReason()) ? existing.getEditableReason() : "此配置不可编辑");
        SysConfigEntity entity = new SysConfigEntity();
        entity.setId(id);
        entity.setConfigValue(value);
        return sysConfigMapper.updateById(entity) > 0;
    }

    @Override
    public String getValue(String configKey) {
        SysConfigEntity entity = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfigEntity>()
                .eq(SysConfigEntity::getConfigKey, configKey));
        return entity == null ? null : entity.getConfigValue();
    }

    @Override
    public Boolean getBooleanValue(String configKey, Boolean defaultValue) {
        String value = getValue(configKey);
        return StringUtils.hasText(value) ? Boolean.valueOf(value) : defaultValue;
    }

    @Override
    public Integer getIntegerValue(String configKey, Integer defaultValue) {
        String value = getValue(configKey);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return Require.fail(SystemCode.SYSTEM_INVALID, "配置值不是整数: " + configKey, exception);
        }
    }

    @Override
    public List<String> listTypes() {
        return Arrays.stream(ConfigTypeEnum.values()).map(ConfigTypeEnum::name).toList();
    }

    @Override
    public List<String> listValueTypes() {
        return Arrays.stream(ConfigValueTypeEnum.values()).map(ConfigValueTypeEnum::name).toList();
    }

    private SysConfigEntity requireConfig(Long id) {
        SysConfigEntity entity = sysConfigMapper.selectById(id);
        Require.notNull(entity, SystemCode.CONFIG_NOT_FOUND);
        return entity;
    }

    private void copy(SaveSysConfigCommand command, SysConfigEntity entity) {
        entity.setConfigKey(command.getConfigKey());
        entity.setConfigValue(command.getConfigValue());
        entity.setConfigName(command.getConfigName());
        entity.setType(command.getType());
        entity.setDomainCode(StringUtils.hasText(command.getDomainCode()) ? command.getDomainCode().trim() : "COMMON");
        entity.setValueType(command.getValueType() == null ? ConfigValueTypeEnum.STRING : command.getValueType());
        entity.setGroupCode(command.getGroupCode());
        entity.setGroupName(command.getGroupName());
        entity.setDefaultValue(command.getDefaultValue());
        entity.setOptions(command.getOptions());
        entity.setOptionSource(command.getOptionSource() == null ? ConfigOptionSourceEnum.CUSTOM : command.getOptionSource());
        entity.setDictType(command.getDictType());
        entity.setEditable(command.getEditable() == null || command.getEditable());
        entity.setEditableReason(command.getEditableReason());
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        entity.setRemark(command.getRemark());
    }

    private SysConfigVO toVO(SysConfigEntity entity) {
        SysConfigVO vo = new SysConfigVO();
        vo.setId(entity.getId());
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigValue(entity.getConfigValue());
        vo.setConfigName(entity.getConfigName());
        vo.setType(entity.getType());
        vo.setDomainCode(entity.getDomainCode());
        vo.setValueType(entity.getValueType() == null ? ConfigValueTypeEnum.STRING : entity.getValueType());
        vo.setGroupCode(entity.getGroupCode());
        vo.setGroupName(entity.getGroupName());
        vo.setDefaultValue(entity.getDefaultValue());
        vo.setOptions(entity.getOptions());
        vo.setOptionSource(entity.getOptionSource() == null ? ConfigOptionSourceEnum.CUSTOM : entity.getOptionSource());
        vo.setDictType(entity.getDictType());
        vo.setEditable(entity.getEditable() == null || entity.getEditable());
        vo.setEditableReason(entity.getEditableReason());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
