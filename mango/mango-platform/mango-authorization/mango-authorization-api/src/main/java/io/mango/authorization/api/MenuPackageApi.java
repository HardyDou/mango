package io.mango.authorization.api;

import io.mango.authorization.api.command.MenuPackageCommand;
import io.mango.authorization.api.query.MenuPackageQuery;
import io.mango.authorization.api.vo.MenuPackageVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 菜单授权套餐 API 契约。
 */
public interface MenuPackageApi {

    R<List<MenuPackageVO>> list(@Valid MenuPackageQuery query);

    R<MenuPackageVO> detail(@Positive Long packageId);

    R<Long> create(@Valid MenuPackageCommand command);

    R<Boolean> update(@Valid MenuPackageCommand command);

    R<Boolean> delete(@Positive Long packageId);
}
