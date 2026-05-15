package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.MenuPackageCommand;
import io.mango.authorization.api.vo.MenuPackageVO;
import io.mango.authorization.core.entity.MenuPackage;
import io.mango.authorization.core.entity.MenuPackageItem;
import io.mango.authorization.core.mapper.MenuPackageItemMapper;
import io.mango.authorization.core.mapper.MenuPackageMapper;
import io.mango.authorization.core.service.IMenuPackageService;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuPackageServiceImpl implements IMenuPackageService {

    private final MenuPackageMapper menuPackageMapper;
    private final MenuPackageItemMapper menuPackageItemMapper;

    @Override
    public List<MenuPackageVO> listPackages(String appCode, String keyword, Integer status) {
        LambdaQueryWrapper<MenuPackage> wrapper = new LambdaQueryWrapper<MenuPackage>()
                .eq(StringUtils.hasText(appCode), MenuPackage::getAppCode, appCode)
                .eq(status != null, MenuPackage::getStatus, status)
                .orderByAsc(MenuPackage::getSort)
                .orderByAsc(MenuPackage::getPackageId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(MenuPackage::getPackageName, keyword)
                    .or()
                    .like(MenuPackage::getPackageCode, keyword));
        }
        return menuPackageMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .peek(vo -> vo.setMenuIds(listMenuIds(vo.getPackageId())))
                .toList();
    }

    @Override
    public MenuPackageVO getById(Long packageId) {
        MenuPackage entity = menuPackageMapper.selectById(packageId);
        if (entity == null) {
            return null;
        }
        MenuPackageVO vo = toVo(entity);
        vo.setMenuIds(listMenuIds(packageId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MenuPackageCommand command) {
        validateUniqueCode(command.getPackageCode(), null);
        MenuPackage entity = toEntity(command);
        menuPackageMapper.insert(entity);
        saveItems(entity.getPackageId(), command.getMenuIds());
        return entity.getPackageId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(MenuPackageCommand command) {
        Require.notNull(command.getPackageId(), "packageId不能为空");
        validateUniqueCode(command.getPackageCode(), command.getPackageId());
        boolean updated = menuPackageMapper.updateById(toEntity(command)) > 0;
        if (updated) {
            menuPackageItemMapper.delete(new LambdaQueryWrapper<MenuPackageItem>()
                    .eq(MenuPackageItem::getPackageId, command.getPackageId()));
            saveItems(command.getPackageId(), command.getMenuIds());
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long packageId) {
        menuPackageItemMapper.delete(new LambdaQueryWrapper<MenuPackageItem>()
                .eq(MenuPackageItem::getPackageId, packageId));
        return menuPackageMapper.deleteById(packageId) > 0;
    }

    @Override
    public List<Long> listMenuIds(Long packageId) {
        return menuPackageItemMapper.selectList(new LambdaQueryWrapper<MenuPackageItem>()
                        .eq(MenuPackageItem::getPackageId, packageId)
                        .orderByAsc(MenuPackageItem::getSort)
                        .orderByAsc(MenuPackageItem::getId))
                .stream()
                .map(MenuPackageItem::getMenuId)
                .toList();
    }

    private void saveItems(Long packageId, List<Long> menuIds) {
        for (int i = 0; i < menuIds.size(); i++) {
            MenuPackageItem item = new MenuPackageItem();
            item.setPackageId(packageId);
            item.setMenuId(menuIds.get(i));
            item.setSort(i + 1);
            menuPackageItemMapper.insert(item);
        }
    }

    private void validateUniqueCode(String packageCode, Long excludeId) {
        MenuPackage existing = menuPackageMapper.selectOne(new LambdaQueryWrapper<MenuPackage>()
                .eq(MenuPackage::getPackageCode, packageCode)
                .ne(excludeId != null, MenuPackage::getPackageId, excludeId)
                .last("LIMIT 1"));
        Require.isTrue(existing == null, "套餐编码已存在");
    }

    private MenuPackage toEntity(MenuPackageCommand command) {
        MenuPackage entity = new MenuPackage();
        entity.setPackageId(command.getPackageId());
        entity.setPackageName(command.getPackageName());
        entity.setPackageCode(command.getPackageCode());
        entity.setAppCode(command.getAppCode());
        entity.setStatus(command.getStatus());
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setRemark(command.getRemark());
        entity.setDelFlag(0);
        return entity;
    }

    private MenuPackageVO toVo(MenuPackage entity) {
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
