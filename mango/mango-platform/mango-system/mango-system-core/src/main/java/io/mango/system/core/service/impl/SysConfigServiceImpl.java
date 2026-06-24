package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.R;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.mango.system.api.po.SysConfigPo;
import io.mango.system.core.entity.SysConfig;
import io.mango.system.core.mapper.SysConfigMapper;
import io.mango.system.core.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements ISysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private static final int DISABLED = 0;

    @Override
    public R<List<SysConfigPo>> list(ConfigTypeEnum type, String domainCode) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(SysConfig::getType, type);
        }
        if (StringUtils.hasText(domainCode)) {
            wrapper.eq(SysConfig::getDomainCode, domainCode.trim());
        }
        wrapper.orderByAsc(SysConfig::getSort);
        List<SysConfig> list = sysConfigMapper.selectList(wrapper);
        List<SysConfigPo> poList = list.stream().map(this::convertToPo).collect(Collectors.toList());
        return R.ok(poList);
    }

    @Override
    public R<SysConfigPo> get(Long id) {
        SysConfig entity = sysConfigMapper.selectById(id);
        if (entity == null) {
            return R.fail("配置不存在");
        }
        return R.ok(convertToPo(entity));
    }

    @Override
    public R<Long> create(SysConfigPo po) {
        SysConfig entity = new SysConfig();
        entity.setConfigKey(po.getConfigKey());
        entity.setConfigValue(po.getConfigValue());
        entity.setConfigName(po.getConfigName());
        entity.setType(po.getType());
        entity.setDomainCode(resolveDomainCode(po.getDomainCode()));
        applyPanelMetadata(entity, po);
        entity.setSort(po.getSort());
        entity.setStatus(po.getStatus());
        entity.setRemark(po.getRemark());
        sysConfigMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> update(SysConfigPo po) {
        if (po.getId() == null) {
            return R.fail("ID不能为空");
        }
        SysConfig entity = new SysConfig();
        entity.setId(po.getId());
        entity.setConfigKey(po.getConfigKey());
        entity.setConfigValue(po.getConfigValue());
        entity.setConfigName(po.getConfigName());
        entity.setType(po.getType());
        entity.setDomainCode(resolveDomainCode(po.getDomainCode()));
        applyPanelMetadata(entity, po);
        entity.setSort(po.getSort());
        entity.setStatus(po.getStatus());
        entity.setRemark(po.getRemark());
        return R.ok(sysConfigMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> delete(Long id) {
        return R.ok(sysConfigMapper.deleteById(id) > 0);
    }

    @Override
    public R<Boolean> updateValue(Long id, String value) {
        SysConfig existing = sysConfigMapper.selectById(id);
        if (existing == null) {
            return R.fail("配置不存在");
        }
        if (Integer.valueOf(DISABLED).equals(existing.getStatus())) {
            return R.fail("配置已禁用");
        }
        if (Boolean.FALSE.equals(existing.getEditable())) {
            return R.fail(StringUtils.hasText(existing.getEditableReason())
                    ? existing.getEditableReason()
                    : "此配置不可编辑");
        }
        SysConfig entity = new SysConfig();
        entity.setId(id);
        entity.setConfigValue(value);
        return R.ok(sysConfigMapper.updateById(entity) > 0);
    }

    @Override
    public R<String> getValue(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig entity = sysConfigMapper.selectOne(wrapper);
        if (entity == null) {
            return R.fail("配置不存在");
        }
        return R.ok(entity.getConfigValue());
    }

    @Override
    public R<Boolean> getBooleanValue(String configKey, Boolean defaultValue) {
        R<String> result = getValue(configKey);
        if (!result.isSuccess()) {
            return R.ok(defaultValue);
        }
        return R.ok(Boolean.valueOf(result.getData()));
    }

    @Override
    public R<Integer> getIntegerValue(String configKey, Integer defaultValue) {
        R<String> result = getValue(configKey);
        if (!result.isSuccess() || !StringUtils.hasText(result.getData())) {
            return R.ok(defaultValue);
        }
        try {
            return R.ok(Integer.valueOf(result.getData()));
        } catch (NumberFormatException ex) {
            return R.fail("配置值不是整数");
        }
    }

    @Override
    public R<List<String>> listTypes() {
        return R.ok(Arrays.stream(ConfigTypeEnum.values())
                .map(ConfigTypeEnum::name)
                .toList());
    }

    @Override
    public R<List<String>> listValueTypes() {
        return R.ok(Arrays.stream(ConfigValueTypeEnum.values())
                .map(ConfigValueTypeEnum::name)
                .toList());
    }

    private SysConfigPo convertToPo(SysConfig entity) {
        SysConfigPo po = new SysConfigPo();
        po.setId(entity.getId());
        po.setConfigKey(entity.getConfigKey());
        po.setConfigValue(entity.getConfigValue());
        po.setConfigName(entity.getConfigName());
        po.setType(entity.getType());
        po.setDomainCode(entity.getDomainCode());
        po.setValueType(entity.getValueType() == null ? ConfigValueTypeEnum.STRING : entity.getValueType());
        po.setGroupCode(entity.getGroupCode());
        po.setGroupName(entity.getGroupName());
        po.setDefaultValue(entity.getDefaultValue());
        po.setOptions(entity.getOptions());
        po.setOptionSource(entity.getOptionSource() == null ? ConfigOptionSourceEnum.CUSTOM : entity.getOptionSource());
        po.setDictType(entity.getDictType());
        po.setEditable(entity.getEditable() == null || entity.getEditable());
        po.setEditableReason(entity.getEditableReason());
        po.setSort(entity.getSort());
        po.setStatus(entity.getStatus());
        po.setRemark(entity.getRemark());
        return po;
    }

    private String resolveDomainCode(String domainCode) {
        return StringUtils.hasText(domainCode) ? domainCode.trim() : "COMMON";
    }

    private void applyPanelMetadata(SysConfig entity, SysConfigPo po) {
        entity.setValueType(po.getValueType() == null ? ConfigValueTypeEnum.STRING : po.getValueType());
        entity.setGroupCode(po.getGroupCode());
        entity.setGroupName(po.getGroupName());
        entity.setDefaultValue(po.getDefaultValue());
        entity.setOptions(po.getOptions());
        entity.setOptionSource(po.getOptionSource() == null ? ConfigOptionSourceEnum.CUSTOM : po.getOptionSource());
        entity.setDictType(po.getDictType());
        entity.setEditable(po.getEditable() == null || po.getEditable());
        entity.setEditableReason(po.getEditableReason());
    }
}
