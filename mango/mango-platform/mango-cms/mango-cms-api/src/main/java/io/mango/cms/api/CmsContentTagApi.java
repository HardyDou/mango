package io.mango.cms.api;

import io.mango.cms.api.command.SaveCmsContentTagCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsContentTagPageQuery;
import io.mango.cms.api.vo.CmsContentTagVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * CMS 内容标签能力契约。
 */
@Validated
public interface CmsContentTagApi {

    /**
     * 分页查询内容标签。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsContentTagVO>> pageContentTags(@Valid CmsContentTagPageQuery query);

    /**
     * 查询内容标签列表。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<List<CmsContentTagVO>> listContentTags(@Valid CmsContentTagPageQuery query);

    /**
     * 查询内容标签详情。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<CmsContentTagVO> detailContentTag(@NotNull(message = "标签 ID 不能为空") Long id);

    /**
     * 创建内容标签。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Long> createContentTag(@Valid SaveCmsContentTagCommand command);

    /**
     * 更新内容标签。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateContentTag(@Valid SaveCmsContentTagCommand command);

    /**
     * 更新内容标签状态。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> updateContentTagStatus(@Valid UpdateCmsStatusCommand command);

    /**
     * 删除内容标签。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deleteContentTag(@NotNull(message = "标签 ID 不能为空") Long id);
}
