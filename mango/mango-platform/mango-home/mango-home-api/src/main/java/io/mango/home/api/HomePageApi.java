package io.mango.home.api;

import io.mango.common.result.R;
import io.mango.home.api.command.CreateHomePageCommand;
import io.mango.home.api.command.HomePageIdCommand;
import io.mango.home.api.command.RenameHomePageCommand;
import io.mango.home.api.command.SaveHomePageLayoutCommand;
import io.mango.home.api.command.SortHomePagesCommand;
import io.mango.home.api.query.ResolveHomePageQuery;
import io.mango.home.api.vo.HomePageVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 当前登录用户首页工作台 API。
 */
public interface HomePageApi {

    /**
     * 查询当前登录用户拥有的首页。
     *
     * @return 用户首页列表。
     */
    R<List<HomePageVO>> listMyPages();

    /**
     * 解析当前应打开的首页。
     *
     * @param query 解析条件。
     * @return 首页页面；没有个人首页时返回内置默认首页。
     */
    R<HomePageVO> resolve(@Valid ResolveHomePageQuery query);

    /**
     * 创建用户首页。
     *
     * @param command 创建命令。
     * @return 创建后的首页。
     */
    R<HomePageVO> create(@Valid CreateHomePageCommand command);

    /**
     * 重命名用户首页。
     *
     * @param command 重命名命令。
     * @return 更新后的首页。
     */
    R<HomePageVO> rename(@Valid RenameHomePageCommand command);

    /**
     * 复制用户首页。
     *
     * @param command 源首页 ID 命令。
     * @return 复制后的首页。
     */
    R<HomePageVO> duplicate(@Valid HomePageIdCommand command);

    /**
     * 保存用户首页布局。
     *
     * @param command 布局保存命令。
     * @return 更新后的首页。
     */
    R<HomePageVO> saveLayout(@Valid SaveHomePageLayoutCommand command);

    /**
     * 首页排序。
     *
     * @param command 排序命令。
     * @return 排序后的首页列表。
     */
    R<List<HomePageVO>> sort(@Valid SortHomePagesCommand command);

    /**
     * 设置默认首页。
     *
     * @param command 首页 ID 命令。
     * @return 当前默认首页。
     */
    R<HomePageVO> setDefault(@Valid HomePageIdCommand command);

    /**
     * 删除用户首页。
     *
     * @param command 首页 ID 命令。
     * @return 删除后的默认首页或内置默认首页。
     */
    R<HomePageVO> delete(@Valid HomePageIdCommand command);
}
