package io.mango.system.core.service;

import io.mango.common.result.R;
import io.mango.system.api.po.SysRoutePo;

import java.util.List;

public interface ISysRouteService {

    R<List<SysRoutePo>> list();

    R<List<SysRoutePo>> list(SysRoutePo query);

    R<List<SysRoutePo>> tree();

    R<SysRoutePo> get(Long id);

    R<Long> create(SysRoutePo po);

    R<Boolean> update(SysRoutePo po);

    R<Boolean> delete(Long id);

    R<Boolean> updateSort(List<Long> ids);
}
