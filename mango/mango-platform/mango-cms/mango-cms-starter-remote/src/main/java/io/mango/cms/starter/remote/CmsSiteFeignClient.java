package io.mango.cms.starter.remote;

import io.mango.cms.api.CmsSiteApi;
import io.mango.cms.api.query.SiteAdvertisementQuery;
import io.mango.cms.api.query.SiteBannerQuery;
import io.mango.cms.api.query.SiteCategoryQuery;
import io.mango.cms.api.query.SiteContentDetailQuery;
import io.mango.cms.api.query.SiteContentPageQuery;
import io.mango.cms.api.query.SiteNavigationQuery;
import io.mango.cms.api.query.SiteResolveQuery;
import io.mango.cms.api.vo.SiteAdvertisementVO;
import io.mango.cms.api.vo.SiteBannerVO;
import io.mango.cms.api.vo.SiteCategoryVO;
import io.mango.cms.api.vo.SiteContentVO;
import io.mango.cms.api.vo.SiteNavigationVO;
import io.mango.cms.api.vo.SiteResolveVO;
import io.mango.cms.api.vo.SiteVO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * CMS 公共站点读取 Feign 适配器。
 */
@FeignClient(name = "mango-cms", contextId = "cmsSiteFeignClient", path = "/cms/open")
public interface CmsSiteFeignClient extends CmsSiteApi {

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/sites/resolve")
    R<SiteResolveVO> resolveSite(@SpringQueryMap SiteResolveQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/sites/detail")
    R<SiteVO> detailSite(@SpringQueryMap SiteResolveQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/site-categories/tree")
    R<List<SiteCategoryVO>> treeCategories(@SpringQueryMap SiteCategoryQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/navigations/list")
    R<List<SiteNavigationVO>> listNavigations(@SpringQueryMap SiteNavigationQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/banners/list")
    R<List<SiteBannerVO>> listBanners(@SpringQueryMap SiteBannerQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/advertisements/list")
    R<List<SiteAdvertisementVO>> listAdvertisements(@SpringQueryMap SiteAdvertisementQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/contents/page")
    R<PageResult<SiteContentVO>> pageContents(@SpringQueryMap SiteContentPageQuery query);

    /**
     * {@inheritDoc}
     *
     * @param query 远程请求参数
     * @return 远程调用结果
     */
    @Override
    @GetMapping("/contents/detail")
    R<SiteContentVO> detailContent(@SpringQueryMap SiteContentDetailQuery query);
}
