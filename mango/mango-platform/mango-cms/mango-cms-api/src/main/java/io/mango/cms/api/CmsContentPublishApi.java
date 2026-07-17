package io.mango.cms.api;

import io.mango.cms.api.command.BatchCmsContentPublishCommand;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.query.CmsContentPublishPageQuery;
import io.mango.cms.api.vo.CmsContentPublishVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * CMS 内容发布能力契约。
 */
@Validated
public interface CmsContentPublishApi {

    /**
     * 分页查询内容发布关系。
     *
     * @param query 请求参数
     * @return 调用结果
     */
    R<PageResult<CmsContentPublishVO>> pagePublishes(@Valid CmsContentPublishPageQuery query);

    /**
     * 批量发布内容。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> publishContents(@Valid BatchCmsContentPublishCommand command);

    /**
     * 下线内容发布关系。
     *
     * @param command 请求参数
     * @return 调用结果
     */
    R<Boolean> offlinePublish(@Valid CmsOfflineCommand command);

    /**
     * 删除内容发布关系。
     *
     * @param id 请求参数
     * @return 调用结果
     */
    R<Boolean> deletePublish(@NotNull(message = "发布关系 ID 不能为空") Long id);
}
