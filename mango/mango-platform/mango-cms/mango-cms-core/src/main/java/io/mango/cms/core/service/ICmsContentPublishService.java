package io.mango.cms.core.service;

import io.mango.cms.api.command.BatchCmsContentPublishCommand;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.query.CmsContentPublishPageQuery;
import io.mango.cms.api.vo.CmsContentPublishVO;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** CMS ContentPublish aggregate contract. */
public interface ICmsContentPublishService {

    /**
     * Executes the CMS pagePublishes domain operation.
     *
     * @param query operation input
     * @return PageResult<CmsContentPublishVO> operation result
     */
    PageResult<CmsContentPublishVO> pagePublishes(@Valid CmsContentPublishPageQuery query);

    /**
     * Executes the CMS publishContents domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean publishContents(@Valid BatchCmsContentPublishCommand command);

    /**
     * Executes the CMS offlinePublish domain operation.
     *
     * @param command operation input
     * @return Boolean operation result
     */
    Boolean offlinePublish(@Valid CmsOfflineCommand command);

    /**
     * Executes the CMS deletePublish domain operation.
     *
     * @param id operation input
     * @return Boolean operation result
     */
    Boolean deletePublish(@NotNull(message = "发布关系 ID 不能为空") Long id);

}
