package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsContentPublishApi;
import io.mango.cms.api.command.BatchCmsContentPublishCommand;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.query.CmsContentPublishPageQuery;
import io.mango.cms.api.vo.CmsContentPublishVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * CMS 内容发布 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsContentPublishFeignClient", path = "/cms")
public interface CmsContentPublishFeignClient extends CmsContentPublishApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/content-publishes/page")
    R<PageResult<CmsContentPublishVO>> pagePublishes(@SpringQueryMap CmsContentPublishPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/content-publishes/publish")
    R<Boolean> publishContents(@RequestBody BatchCmsContentPublishCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/content-publishes/offline")
    R<Boolean> offlinePublish(@RequestBody CmsOfflineCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/content-publishes")
    R<Boolean> deletePublish(@RequestParam("id") Long id);
}
