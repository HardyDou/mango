package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsBannerApi;
import io.mango.cms.api.command.SaveCmsBannerCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsBannerPageQuery;
import io.mango.cms.api.vo.CmsBannerVO;
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
 * CMS Banner 管理 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsBannerFeignClient", path = "/cms")
public interface CmsBannerFeignClient extends CmsBannerApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/banners/page")
    R<PageResult<CmsBannerVO>> pageBanners(@SpringQueryMap CmsBannerPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/banners/detail")
    R<CmsBannerVO> detailBanner(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/banners")
    R<Long> createBanner(@RequestBody SaveCmsBannerCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/banners")
    R<Boolean> updateBanner(@RequestBody SaveCmsBannerCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/banners/status")
    R<Boolean> updateBannerStatus(@RequestBody UpdateCmsStatusCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/banners")
    R<Boolean> deleteBanner(@RequestParam("id") Long id);
}
