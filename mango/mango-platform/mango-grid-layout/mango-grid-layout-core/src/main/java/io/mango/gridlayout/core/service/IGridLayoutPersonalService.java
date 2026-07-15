package io.mango.gridlayout.core.service;

import io.mango.gridlayout.api.command.SaveGridLayoutPersonalCommand;
import io.mango.gridlayout.api.query.GridLayoutPersonalQuery;
import io.mango.gridlayout.api.vo.GridLayoutPersonalVO;

/**
 * 当前登录用户自定义栅格布局服务。
 */
public interface IGridLayoutPersonalService {

    /**
     * 查询当前用户在指定页面的个人布局。
     *
     * @param query 查询条件。
     * @return 已保存的个人布局，不存在时返回 {@code null}。
     */
    GridLayoutPersonalVO getPersonal(GridLayoutPersonalQuery query);

    /**
     * 新增或覆盖当前用户在指定页面的个人布局。
     *
     * @param command 保存命令。
     * @return 保存后的个人布局。
     */
    GridLayoutPersonalVO savePersonal(SaveGridLayoutPersonalCommand command);

    /**
     * 删除当前用户在指定页面的个人布局。
     *
     * @param query 删除条件。
     * @return 是否删除到布局记录。
     */
    boolean deletePersonal(GridLayoutPersonalQuery query);
}
