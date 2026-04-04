package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.common.result.R;
import io.mango.system.api.po.SysTenantPo;
import io.mango.system.core.entity.SysTenant;
import io.mango.system.core.mapper.SysTenantMapper;
import io.mango.system.core.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements ISysTenantService {

    private final SysTenantMapper sysTenantMapper;

    @Override
    public R<List<SysTenantPo>> list() {
        List<SysTenant> list = sysTenantMapper.selectList(null);
        List<SysTenantPo> poList = list.stream().map(this::convertToPo).collect(Collectors.toList());
        return R.ok(poList);
    }

    @Override
    public R<SysTenantPo> get(Long id) {
        SysTenant entity = sysTenantMapper.selectById(id);
        if (entity == null) {
            return R.fail("租户不存在");
        }
        return R.ok(convertToPo(entity));
    }

    @Override
    public R<Long> create(SysTenantPo po) {
        SysTenant entity = new SysTenant();
        entity.setTenantName(po.getTenantName());
        entity.setTenantCode(po.getTenantCode());
        entity.setStatus(po.getStatus());
        entity.setContact(po.getContact());
        entity.setMobile(po.getMobile());
        entity.setEmail(po.getEmail());
        entity.setRemark(po.getRemark());
        sysTenantMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> update(SysTenantPo po) {
        if (po.getId() == null) {
            return R.fail("ID不能为空");
        }
        SysTenant entity = new SysTenant();
        entity.setId(po.getId());
        entity.setTenantName(po.getTenantName());
        entity.setTenantCode(po.getTenantCode());
        entity.setStatus(po.getStatus());
        entity.setContact(po.getContact());
        entity.setMobile(po.getMobile());
        entity.setEmail(po.getEmail());
        entity.setRemark(po.getRemark());
        return R.ok(sysTenantMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> delete(Long id) {
        return R.ok(sysTenantMapper.deleteById(id) > 0);
    }

    @Override
    public R<Boolean> updateStatus(Long id, Integer status) {
        SysTenant entity = new SysTenant();
        entity.setId(id);
        entity.setStatus(status);
        return R.ok(sysTenantMapper.updateById(entity) > 0);
    }

    private SysTenantPo convertToPo(SysTenant entity) {
        SysTenantPo po = new SysTenantPo();
        po.setId(entity.getId());
        po.setTenantName(entity.getTenantName());
        po.setTenantCode(entity.getTenantCode());
        po.setStatus(entity.getStatus());
        po.setContact(entity.getContact());
        po.setMobile(entity.getMobile());
        po.setEmail(entity.getEmail());
        po.setRemark(entity.getRemark());
        return po;
    }
}
