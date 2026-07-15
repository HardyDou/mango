package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.command.MenuPackageCommand;
import io.mango.authorization.api.query.MenuPackageQuery;
import io.mango.authorization.api.vo.MenuPackageVO;
import io.mango.authorization.core.entity.MenuPackageEntity;
import io.mango.authorization.core.entity.MenuPackageItemEntity;
import io.mango.authorization.core.mapper.MenuPackageItemMapper;
import io.mango.authorization.core.mapper.MenuPackageMapper;
import io.mango.authorization.core.service.IMenuPackageService;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuPackageService implements IMenuPackageService {

    private final MenuPackageMapper menuPackageMapper;
    private final MenuPackageItemMapper menuPackageItemMapper;

    @Override
    public List<MenuPackageVO> listPackages(MenuPackageQuery query) {
        LambdaQueryWrapper<MenuPackageEntity> wrapper = new LambdaQueryWrapper<MenuPackageEntity>()
                .eq(StringUtils.hasText(query.getAppCode()), MenuPackageEntity::getAppCode, query.getAppCode())
                .eq(query.getStatus() != null, MenuPackageEntity::getStatus, query.getStatus())
                .orderByAsc(MenuPackageEntity::getSort)
                .orderByAsc(MenuPackageEntity::getId);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(q -> q.like(MenuPackageEntity::getPackageName, query.getKeyword())
                    .or()
                    .like(MenuPackageEntity::getPackageCode, query.getKeyword()));
        }
        return menuPackageMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .peek(vo -> vo.setMenuIds(listMenuIds(vo.getPackageId())))
                .toList();
    }

    @Override
    public MenuPackageVO getById(Long packageId) {
        Require.notNull(packageId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "套餐ID不能为空");
        MenuPackageEntity entity = menuPackageMapper.selectById(packageId);
        Require.notNull(entity, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "套餐不存在");
        MenuPackageVO vo = toVo(entity);
        vo.setMenuIds(listMenuIds(packageId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MenuPackageCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "套餐命令不能为空");
        validateUniqueCode(command.getPackageCode(), null);
        MenuPackageEntity entity = toEntity(command);
        menuPackageMapper.insert(entity);
        saveItems(entity.getPackageId(), entity.getTenantIdAsLong(), command.getMenuIds());
        return entity.getPackageId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(MenuPackageCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "套餐命令不能为空");
        Require.notNull(command.getPackageId(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "packageId不能为空");
        validateUniqueCode(command.getPackageCode(), command.getPackageId());
        MenuPackageEntity current = menuPackageMapper.selectById(command.getPackageId());
        Require.notNull(current, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "套餐不存在");
        boolean updated = menuPackageMapper.updateById(toEntity(command)) > 0;
        Require.isTrue(updated, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "套餐不存在");
        menuPackageItemMapper.delete(new LambdaQueryWrapper<MenuPackageItemEntity>()
                .eq(MenuPackageItemEntity::getPackageId, command.getPackageId()));
        saveItems(command.getPackageId(), current.getTenantIdAsLong(), command.getMenuIds());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long packageId) {
        Require.notNull(packageId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "套餐ID不能为空");
        Require.notNull(menuPackageMapper.selectById(packageId), AuthorizationCode.AUTHORIZATION_NOT_FOUND,
                "套餐不存在");
        menuPackageItemMapper.delete(new LambdaQueryWrapper<MenuPackageItemEntity>()
                .eq(MenuPackageItemEntity::getPackageId, packageId));
        boolean deleted = menuPackageMapper.deleteById(packageId) > 0;
        Require.isTrue(deleted, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "套餐不存在");
        return true;
    }

    @Override
    public List<Long> listMenuIds(Long packageId) {
        return menuPackageItemMapper.selectList(new LambdaQueryWrapper<MenuPackageItemEntity>()
                        .eq(MenuPackageItemEntity::getPackageId, packageId)
                        .orderByAsc(MenuPackageItemEntity::getSort)
                        .orderByAsc(MenuPackageItemEntity::getId))
                .stream()
                .map(MenuPackageItemEntity::getMenuId)
                .toList();
    }

    private void saveItems(Long packageId, Long tenantId, List<Long> menuIds) {
        for (int i = 0; i < menuIds.size(); i++) {
            MenuPackageItemEntity item = new MenuPackageItemEntity();
            item.setTenantId(tenantId);
            item.setPackageId(packageId);
            item.setMenuId(menuIds.get(i));
            item.setSort(i + 1);
            menuPackageItemMapper.insert(item);
        }
    }

    private void validateUniqueCode(String packageCode, Long excludeId) {
        MenuPackageEntity existing = menuPackageMapper.selectOne(new LambdaQueryWrapper<MenuPackageEntity>()
                .eq(MenuPackageEntity::getPackageCode, packageCode)
                .ne(excludeId != null, MenuPackageEntity::getId, excludeId)
                .last("LIMIT 1"));
        Require.isTrue(existing == null, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "套餐编码已存在");
    }

    private MenuPackageEntity toEntity(MenuPackageCommand command) {
        MenuPackageEntity entity = new MenuPackageEntity();
        entity.setPackageId(command.getPackageId());
        entity.setPackageName(command.getPackageName());
        entity.setPackageCode(command.getPackageCode());
        entity.setAppCode(command.getAppCode());
        entity.setStatus(command.getStatus());
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setRemark(command.getRemark());
        entity.setDelFlag(0);
        entity.setTenantId(resolveTenantId(command.getPackageId()));
        return entity;
    }

    private Long resolveTenantId(Long packageId) {
        String contextTenantId = MangoContextHolder.tenantId();
        if (StringUtils.hasText(contextTenantId)) {
            try {
                return Long.parseLong(contextTenantId);
            } catch (NumberFormatException ignored) {
                // Authorization tenant tables use numeric IDs; fall back to the persisted/default tenant below.
            }
        }
        if (packageId != null) {
            MenuPackageEntity existing = menuPackageMapper.selectById(packageId);
            if (existing != null && existing.getTenantIdAsLong() != null) {
                return existing.getTenantIdAsLong();
            }
        }
        return 1L;
    }

    private MenuPackageVO toVo(MenuPackageEntity entity) {
        MenuPackageVO vo = new MenuPackageVO();
        vo.setPackageId(entity.getPackageId());
        vo.setPackageName(entity.getPackageName());
        vo.setPackageCode(entity.getPackageCode());
        vo.setAppCode(entity.getAppCode());
        vo.setStatus(entity.getStatus());
        vo.setSort(entity.getSort());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
