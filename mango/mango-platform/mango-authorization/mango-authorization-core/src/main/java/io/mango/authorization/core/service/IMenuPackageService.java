package io.mango.authorization.core.service;

import io.mango.authorization.api.command.MenuPackageCommand;
import io.mango.authorization.api.vo.MenuPackageVO;

import java.util.List;

public interface IMenuPackageService {

    List<MenuPackageVO> listPackages(String appCode, String keyword, Integer status);

    MenuPackageVO getById(Long packageId);

    Long create(MenuPackageCommand command);

    boolean update(MenuPackageCommand command);

    boolean delete(Long packageId);

    List<Long> listMenuIds(Long packageId);
}
