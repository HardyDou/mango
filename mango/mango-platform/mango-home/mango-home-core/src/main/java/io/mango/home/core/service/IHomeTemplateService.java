package io.mango.home.core.service;

import io.mango.home.api.command.CreateHomeTemplateCommand;
import io.mango.home.api.command.HomeTemplateIdCommand;
import io.mango.home.api.command.SaveHomeTemplateAuthorizationsCommand;
import io.mango.home.api.command.UpdateHomeTemplateDraftCommand;
import io.mango.home.api.command.UpdateHomeTemplateStatusCommand;
import io.mango.home.api.query.HomeTemplateAuthorizationQuery;
import io.mango.home.api.query.HomeTemplateQuery;
import io.mango.home.api.query.UserHomeViewQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.api.vo.HomeTemplateAuthorizationVO;
import io.mango.home.api.vo.HomeTemplateVO;

import java.util.List;

/** 首页模板、版本和授权领域服务。 */
public interface IHomeTemplateService {

    /**
     * 查询当前租户的首页模板。
     *
     * @param query 查询条件
     * @return 首页模板列表
     */
    List<HomeTemplateVO> list(HomeTemplateQuery query);

    /**
     * 查询首页模板详情。
     *
     * @param id 模板ID
     * @return 首页模板详情
     */
    HomeTemplateVO detail(Long id);

    /**
     * 创建首页模板草稿。
     *
     * @param command 创建命令
     * @return 创建后的首页模板
     */
    HomeTemplateVO create(CreateHomeTemplateCommand command);

    /**
     * 更新首页模板草稿。
     *
     * @param command 草稿更新命令
     * @return 更新后的首页模板
     */
    HomeTemplateVO updateDraft(UpdateHomeTemplateDraftCommand command);

    /**
     * 复制首页模板。
     *
     * @param command 模板ID命令
     * @return 复制后的首页模板
     */
    HomeTemplateVO copy(HomeTemplateIdCommand command);

    /**
     * 发布首页模板草稿。
     *
     * @param command 模板ID命令
     * @return 发布后的首页模板
     */
    HomeTemplateVO publish(HomeTemplateIdCommand command);

    /**
     * 更新首页模板启用状态。
     *
     * @param command 状态更新命令
     * @return 更新后的首页模板
     */
    HomeTemplateVO updateStatus(UpdateHomeTemplateStatusCommand command);

    /**
     * 删除未授权的首页模板。
     *
     * @param command 模板ID命令
     */
    void delete(HomeTemplateIdCommand command);

    /**
     * 查询首页模板授权项。
     *
     * @param query 授权查询条件
     * @return 授权项列表
     */
    List<HomeTemplateAuthorizationVO> listAuthorizations(HomeTemplateAuthorizationQuery query);

    /**
     * 覆盖保存首页模板授权项。
     *
     * @param command 授权保存命令
     * @return 保存后的授权项列表
     */
    List<HomeTemplateAuthorizationVO> saveAuthorizations(SaveHomeTemplateAuthorizationsCommand command);

    /**
     * 查询指定用户最终可见的首页集合。
     *
     * @param query 用户首页查询条件
     * @return 用户可见首页列表
     */
    List<HomePageVO> resolveUserPages(UserHomeViewQuery query);
}
