package io.mango.system.core.service;

import io.mango.common.result.R;
import io.mango.system.api.po.SysTenantPo;

import java.util.List;

public interface ISysTenantService {

    R<List<SysTenantPo>> list();

    R<SysTenantPo> get(Long id);

    R<Long> create(SysTenantPo po);

    R<Boolean> update(SysTenantPo po);

    R<Boolean> delete(Long id);

    R<Boolean> updateStatus(Long id, Integer status);
}
