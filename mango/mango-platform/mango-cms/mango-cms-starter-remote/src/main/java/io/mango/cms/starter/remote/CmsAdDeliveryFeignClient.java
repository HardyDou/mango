package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsAdDeliveryApi;
import io.mango.cms.api.command.SaveCmsAdDeliveryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdDeliveryPageQuery;
import io.mango.cms.api.vo.CmsAdDeliveryVO;
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
 * CMS 广告投放 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsAdDeliveryFeignClient", path = "/cms")
public interface CmsAdDeliveryFeignClient extends CmsAdDeliveryApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/ad-deliveries/page")
    R<PageResult<CmsAdDeliveryVO>> pageAdDeliveries(@SpringQueryMap CmsAdDeliveryPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/ad-deliveries/detail")
    R<CmsAdDeliveryVO> detailAdDelivery(@RequestParam("id") Long id);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PostMapping("/ad-deliveries")
    R<Long> createAdDelivery(@RequestBody SaveCmsAdDeliveryCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/ad-deliveries")
    R<Boolean> updateAdDelivery(@RequestBody SaveCmsAdDeliveryCommand command);

    /**
     * {@inheritDoc}
     *
     * @param command 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @PutMapping("/ad-deliveries/status")
    R<Boolean> updateAdDeliveryStatus(@RequestBody UpdateCmsStatusCommand command);

    /**
     * {@inheritDoc}
     *
     * @param id 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @DeleteMapping("/ad-deliveries")
    R<Boolean> deleteAdDelivery(@RequestParam("id") Long id);
}
