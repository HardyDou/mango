package io.mango.home.core.service;

import io.mango.home.api.command.CreateHomePageCommand;
import io.mango.home.api.command.RenameHomePageCommand;
import io.mango.home.api.command.SaveHomePageLayoutCommand;
import io.mango.home.api.command.SortHomePagesCommand;
import io.mango.home.api.query.ResolveHomePageQuery;
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
     * @param id 首页ID
     * @param command 重命名命令
     * @return 首页信息
     */
    HomePageVO rename(Long id, RenameHomePageCommand command);

    /**
     * 复制首页。
     *
     * @param id 首页ID
     * @return 复制后的首页信息
     */
    HomePageVO duplicate(Long id);

    /**
     * 保存首页布局。
     *
     * @param id 首页ID
     * @param command 布局保存命令
     * @return 首页信息
     */
    HomePageVO saveLayout(Long id, SaveHomePageLayoutCommand command);

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
     * @param id 首页ID
     * @return 首页信息
     */
    HomePageVO setDefault(Long id);

    /**
     * 删除首页。
     *
     * @param id 首页ID
     * @return 删除后解析出的首页信息
     */
    HomePageVO delete(Long id);
}
