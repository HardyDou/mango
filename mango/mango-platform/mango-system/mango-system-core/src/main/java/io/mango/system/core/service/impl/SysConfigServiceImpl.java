package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.R;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.po.SysConfigPo;
import io.mango.system.core.entity.SysConfig;
import io.mango.system.core.mapper.SysConfigMapper;
import io.mango.system.core.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements ISysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public R<List<SysConfigPo>> list(ConfigTypeEnum type) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(SysConfig::getType, type);
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
    public R<List<String>> listTypes() {
        return R.ok(Arrays.stream(ConfigTypeEnum.values())
                .map(ConfigTypeEnum::name)
                .toList());
    }

    private SysConfigPo convertToPo(SysConfig entity) {
        SysConfigPo po = new SysConfigPo();
        po.setId(entity.getId());
        po.setConfigKey(entity.getConfigKey());
        po.setConfigValue(entity.getConfigValue());
        po.setConfigName(entity.getConfigName());
        po.setType(entity.getType());
        po.setSort(entity.getSort());
        po.setStatus(entity.getStatus());
        po.setRemark(entity.getRemark());
        return po;
    }
}
