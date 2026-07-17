package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsAdvertisementApi;
import io.mango.cms.api.command.SaveCmsAdvertisementCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdvertisementPageQuery;
import io.mango.cms.api.vo.CmsAdvertisementVO;
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
 * CMS 广告位管理 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsAdvertisementFeignClient", path = "/cms")
public interface CmsAdvertisementFeignClient extends CmsAdvertisementApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/advertisements/page")
    R<PageResult<CmsAdvertisementVO>> pageAdvertisements(@SpringQueryMap CmsAdvertisementPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/advertisements/detail")
    R<CmsAdvertisementVO> detailAdvertisement(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/advertisements")
    R<Long> createAdvertisement(@RequestBody SaveCmsAdvertisementCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/advertisements")
    R<Boolean> updateAdvertisement(@RequestBody SaveCmsAdvertisementCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/advertisements/status")
    R<Boolean> updateAdvertisementStatus(@RequestBody UpdateCmsStatusCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/advertisements")
    R<Boolean> deleteAdvertisement(@RequestParam("id") Long id);
}
