package io.mango.cms.core.service;

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
import io.mango.common.vo.PageResult;
import io.mango.file.api.vo.FileDownloadVO;
import jakarta.validation.Valid;

import java.util.List;

public interface ICmsSiteService {

    /**
     * Executes the CMS resolveSite domain operation.
     *
     * @param query operation input
     * @return SiteResolveVO operation result
     */
    SiteResolveVO resolveSite(@Valid SiteResolveQuery query);

    /**
     * Executes the CMS detailSite domain operation.
     *
     * @param query operation input
     * @return SiteVO operation result
     */
    SiteVO detailSite(@Valid SiteResolveQuery query);

    /**
     * Executes the CMS treeCategories domain operation.
     *
     * @param query operation input
     * @return List<SiteCategoryVO> operation result
     */
    List<SiteCategoryVO> treeCategories(@Valid SiteCategoryQuery query);

    /**
     * Executes the CMS listNavigations domain operation.
     *
     * @param query operation input
     * @return List<SiteNavigationVO> operation result
     */
    List<SiteNavigationVO> listNavigations(@Valid SiteNavigationQuery query);

    /**
     * Executes the CMS listBanners domain operation.
     *
     * @param query operation input
     * @return List<SiteBannerVO> operation result
     */
    List<SiteBannerVO> listBanners(@Valid SiteBannerQuery query);

    /**
     * Executes the CMS listAdvertisements domain operation.
     *
     * @param query operation input
     * @return List<SiteAdvertisementVO> operation result
     */
    List<SiteAdvertisementVO> listAdvertisements(@Valid SiteAdvertisementQuery query);

    /**
     * Executes the CMS pageContents domain operation.
     *
     * @param query operation input
     * @return PageResult<SiteContentVO> operation result
     */
    PageResult<SiteContentVO> pageContents(@Valid SiteContentPageQuery query);

    /**
     * Executes the CMS detailContent domain operation.
     *
     * @param query operation input
     * @return SiteContentVO operation result
     */
    SiteContentVO detailContent(@Valid SiteContentDetailQuery query);

    /**
     * Executes the CMS publicFile domain operation.
     *
     * @param id operation input
     * @param query operation input
     * @return FileDownloadVO operation result
     */
    FileDownloadVO publicFile(Long id, SiteResolveQuery query);
}
