package io.mango.gridlayout.api;

import io.mango.common.result.R;
import io.mango.gridlayout.api.command.SaveGridLayoutPersonalCommand;
import io.mango.gridlayout.api.query.GridLayoutPersonalQuery;
import io.mango.gridlayout.api.vo.GridLayoutPersonalVO;
import jakarta.validation.Valid;

/**
 * 当前登录用户自定义栅格布局 API。
 */
public interface GridLayoutPersonalApi {

    /**
     * 查询当前登录用户在指定页面的布局。
     *
     * @param query 查询条件。
     * @return 当前用户布局，不存在时返回 null。
     */
    R<GridLayoutPersonalVO> getPersonal(@Valid GridLayoutPersonalQuery query);

    /**
     * 保存当前登录用户在指定页面的布局。
     *
     * @param command 保存命令。
     * @return 保存后的布局。
     */
    R<GridLayoutPersonalVO> savePersonal(@Valid SaveGridLayoutPersonalCommand command);

    /**
     * 删除当前登录用户在指定页面的布局。
     *
     * @param query 删除条件。
     * @return 是否删除成功。
     */
    R<Boolean> deletePersonal(@Valid GridLayoutPersonalQuery query);
}
