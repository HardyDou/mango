package io.mango.cms.api;

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
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface CmsSiteApi {

    R<SiteResolveVO> resolveSite(@Valid SiteResolveQuery query);

    R<SiteVO> detailSite(@Valid SiteResolveQuery query);

    R<List<SiteCategoryVO>> treeCategories(@Valid SiteCategoryQuery query);

    R<List<SiteNavigationVO>> listNavigations(@Valid SiteNavigationQuery query);

    R<List<SiteBannerVO>> listBanners(@Valid SiteBannerQuery query);

    R<List<SiteAdvertisementVO>> listAdvertisements(@Valid SiteAdvertisementQuery query);

    R<PageResult<SiteContentVO>> pageContents(@Valid SiteContentPageQuery query);

    R<SiteContentVO> detailContent(@Valid SiteContentDetailQuery query);
}
