package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.R;
import io.mango.system.api.po.SysRoutePo;
import io.mango.system.core.entity.SysRoute;
import io.mango.system.core.mapper.SysRouteMapper;
import io.mango.system.core.service.ISysRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRouteServiceImpl implements ISysRouteService {

    private final SysRouteMapper sysRouteMapper;

    @Override
    public R<List<SysRoutePo>> list() {
        return list(new SysRoutePo());
    }

    @Override
    public R<List<SysRoutePo>> list(SysRoutePo query) {
        LambdaQueryWrapper<SysRoute> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            String routeName = StringUtils.hasText(query.getRouteName()) ? query.getRouteName().trim() : null;
            String routePath = StringUtils.hasText(query.getRoutePath()) ? query.getRoutePath().trim() : null;
            if (routeName != null && routeName.equals(routePath)) {
                wrapper.and(item -> item.like(SysRoute::getRouteName, routeName)
                        .or()
                        .like(SysRoute::getRoutePath, routePath));
            } else {
                if (routeName != null) {
                    wrapper.like(SysRoute::getRouteName, routeName);
                }
                if (routePath != null) {
                    wrapper.like(SysRoute::getRoutePath, routePath);
                }
            }
            if (query.getRouteType() != null) {
                wrapper.eq(SysRoute::getRouteType, query.getRouteType());
            }
            if (query.getStatus() != null) {
                wrapper.eq(SysRoute::getStatus, query.getStatus());
            }
        }
        wrapper.orderByAsc(SysRoute::getSort);
        List<SysRoute> list = sysRouteMapper.selectList(wrapper);
        List<SysRoutePo> poList = list.stream().map(this::convertToPo).collect(Collectors.toList());
        return R.ok(poList);
    }

    @Override
    public R<List<SysRoutePo>> tree() {
        return list();
    }

    @Override
    public R<SysRoutePo> get(Long id) {
        SysRoute entity = sysRouteMapper.selectById(id);
        if (entity == null) {
            return R.fail("路由不存在");
        }
        return R.ok(convertToPo(entity));
    }

    @Override
    public R<Long> create(SysRoutePo po) {
        SysRoute entity = new SysRoute();
        entity.setRouteName(po.getRouteName());
        entity.setRouteType(po.getRouteType());
        entity.setRoutePath(po.getRoutePath());
        entity.setRouteDesc(po.getRouteDesc());
        entity.setSort(po.getSort());
        entity.setStatus(po.getStatus());
        sysRouteMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    public R<Boolean> update(SysRoutePo po) {
        if (po.getId() == null) {
            return R.fail("ID不能为空");
        }
        SysRoute entity = new SysRoute();
        entity.setId(po.getId());
        entity.setRouteName(po.getRouteName());
        entity.setRouteType(po.getRouteType());
        entity.setRoutePath(po.getRoutePath());
        entity.setRouteDesc(po.getRouteDesc());
        entity.setSort(po.getSort());
        entity.setStatus(po.getStatus());
        return R.ok(sysRouteMapper.updateById(entity) > 0);
    }

    @Override
    public R<Boolean> delete(Long id) {
        return R.ok(sysRouteMapper.deleteById(id) > 0);
    }

    @Override
    public R<Boolean> updateSort(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.fail("路由ID列表不能为空");
        }
        for (int i = 0; i < ids.size(); i++) {
            SysRoute entity = new SysRoute();
            entity.setId(ids.get(i));
            entity.setSort(i + 1);
            sysRouteMapper.updateById(entity);
        }
        return R.ok(true);
    }

    private SysRoutePo convertToPo(SysRoute entity) {
        SysRoutePo po = new SysRoutePo();
        po.setId(entity.getId());
        po.setRouteName(entity.getRouteName());
        po.setRouteType(entity.getRouteType());
        po.setRoutePath(entity.getRoutePath());
        po.setRouteDesc(entity.getRouteDesc());
        po.setSort(entity.getSort());
        po.setStatus(entity.getStatus());
        return po;
    }
}
