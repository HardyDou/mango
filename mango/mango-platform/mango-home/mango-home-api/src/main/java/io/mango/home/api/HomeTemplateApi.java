package io.mango.home.api;

import io.mango.common.result.R;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 首页模板、版本与授权管理 API。 */
public interface HomeTemplateApi {

    /**
     * 查询当前租户的首页模板。
     *
     * @param query 查询条件
     * @return 首页模板列表
     */
    R<List<HomeTemplateVO>> list(@Valid HomeTemplateQuery query);

    /**
     * 查询首页模板详情。
     *
     * @param id 模板ID
     * @return 首页模板详情
     */
    R<HomeTemplateVO> detail(@NotNull(message = "模板ID不能为空") Long id);

    /**
     * 创建首页模板草稿。
     *
     * @param command 创建命令
     * @return 创建后的首页模板
     */
    R<HomeTemplateVO> create(@Valid CreateHomeTemplateCommand command);

    /**
     * 更新首页模板草稿。
     *
     * @param command 草稿更新命令
     * @return 更新后的首页模板
     */
    R<HomeTemplateVO> updateDraft(@Valid UpdateHomeTemplateDraftCommand command);

    /**
     * 复制首页模板。
     *
     * @param command 模板ID命令
     * @return 复制后的首页模板
     */
    R<HomeTemplateVO> copy(@Valid HomeTemplateIdCommand command);

    /**
     * 发布首页模板草稿。
     *
     * @param command 模板ID命令
     * @return 发布后的首页模板
     */
    R<HomeTemplateVO> publish(@Valid HomeTemplateIdCommand command);

    /**
     * 更新首页模板启用状态。
     *
     * @param command 状态更新命令
     * @return 更新后的首页模板
     */
    R<HomeTemplateVO> updateStatus(@Valid UpdateHomeTemplateStatusCommand command);

    /**
     * 删除未授权首页模板。
     *
     * @param command 模板ID命令
     * @return 空结果
     */
    R<Void> delete(@Valid HomeTemplateIdCommand command);

    /**
     * 查询首页模板授权项。
     *
     * @param query 授权查询条件
     * @return 授权项列表
     */
    R<List<HomeTemplateAuthorizationVO>> listAuthorizations(@Valid HomeTemplateAuthorizationQuery query);

    /**
     * 覆盖保存首页模板授权项。
     *
     * @param command 授权保存命令
     * @return 保存后的授权项列表
     */
    R<List<HomeTemplateAuthorizationVO>> saveAuthorizations(@Valid SaveHomeTemplateAuthorizationsCommand command);

    /**
     * 查询指定用户最终可见的首页集合。
     *
     * @param query 用户首页查询条件
     * @return 用户可见首页列表
     */
    R<List<HomePageVO>> resolveUserPages(@Valid UserHomeViewQuery query);
}
