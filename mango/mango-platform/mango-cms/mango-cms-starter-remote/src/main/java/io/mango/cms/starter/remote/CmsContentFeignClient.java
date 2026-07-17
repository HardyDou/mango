package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsContentApi;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.command.SaveCmsContentCommand;
import io.mango.cms.api.command.UpdateCmsContentReviewCommand;
import io.mango.cms.api.query.CmsContentPageQuery;
import io.mango.cms.api.vo.CmsContentVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * CMS 内容管理 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsContentFeignClient", path = "/cms")
public interface CmsContentFeignClient extends CmsContentApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/contents/page")
    R<PageResult<CmsContentVO>> pageContents(@SpringQueryMap CmsContentPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/contents/detail")
    R<CmsContentVO> detailContent(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/contents")
    R<Long> createContent(@RequestBody SaveCmsContentCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/contents")
    R<Boolean> updateContent(@RequestBody SaveCmsContentCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/contents/submit")
    R<Boolean> submitContent(@RequestBody CmsOfflineCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/contents/approve")
    R<Boolean> approveContent(@RequestBody UpdateCmsContentReviewCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/contents/reject")
    R<Boolean> rejectContent(@RequestBody UpdateCmsContentReviewCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/contents/offline")
    R<Boolean> offlineContent(@RequestBody CmsOfflineCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/contents")
    R<Boolean> deleteContent(@RequestParam("id") Long id);
}
