package io.mango.home.core.service;

import io.mango.common.vo.PageResult;
import io.mango.home.api.command.BatchDeleteHomePagesCommand;
import io.mango.home.api.command.CreateHomePageCommand;
import io.mango.home.api.command.HomePageIdCommand;
import io.mango.home.api.command.RenameHomePageCommand;
import io.mango.home.api.command.SaveHomePageLayoutCommand;
import io.mango.home.api.command.SetDefaultHomePageCommand;
import io.mango.home.api.command.SortHomePagesCommand;
import io.mango.home.api.query.ResolveHomePageQuery;
import io.mango.home.api.query.UserHomePageQuery;
import io.mango.home.api.vo.HomePageVO;

import java.util.List;

public interface IHomePageService {

    /**
     * 查询当前用户可用首页。
     *
     * @return 当前用户首页列表
     */
    List<HomePageVO> listMyPages();

    /**
     * 分页查询当前租户下的用户自定义首页。
     *
     * @param query 查询条件
     * @return 用户自定义首页分页列表
     */
    PageResult<HomePageVO> pageUserPages(UserHomePageQuery query);

    /**
     * 解析当前用户要打开的首页。
     *
     * @param query 首页解析查询
     * @return 首页信息
     */
    HomePageVO resolve(ResolveHomePageQuery query);

    /**
     * 创建当前用户首页。
     *
     * @param command 创建命令
     * @return 首页信息
     */
    HomePageVO create(CreateHomePageCommand command);

    /**
     * 重命名首页。
     *
     * @param command 重命名命令
     * @return 首页信息
     */
    HomePageVO rename(RenameHomePageCommand command);

    /**
     * 复制首页。
     *
     * @param command 首页ID命令
     * @return 复制后的首页信息
     */
    HomePageVO duplicate(HomePageIdCommand command);

    /**
     * 保存首页布局。
     *
     * @param command 布局保存命令
     * @return 首页信息
     */
    HomePageVO saveLayout(SaveHomePageLayoutCommand command);

    /**
     * 保存首页排序。
     *
     * @param command 排序命令
     * @return 排序后的首页列表
     */
    List<HomePageVO> sort(SortHomePagesCommand command);

    /**
     * 设置默认首页。
     *
     * @param command 首页ID命令
     * @return 首页信息
     */
    HomePageVO setDefault(SetDefaultHomePageCommand command);

    /**
     * 删除首页。
     *
     * @param command 首页ID命令
     * @return 删除后解析出的首页信息
     */
    HomePageVO delete(HomePageIdCommand command);

    /**
     * 后台重命名租户内用户首页。
     *
     * @param command 重命名命令
     * @return 首页信息
     */
    HomePageVO adminRename(RenameHomePageCommand command);

    /**
     * 后台保存租户内用户首页布局。
     *
     * @param command 布局保存命令
     * @return 首页信息
     */
    HomePageVO adminSaveLayout(SaveHomePageLayoutCommand command);

    /**
     * 后台删除租户内用户首页。
     *
     * @param command 首页ID命令
     */
    void adminDelete(HomePageIdCommand command);

    /**
     * 后台批量删除租户内用户首页。
     *
     * @param command 批量删除命令
     */
    void adminBatchDelete(BatchDeleteHomePagesCommand command);
}
