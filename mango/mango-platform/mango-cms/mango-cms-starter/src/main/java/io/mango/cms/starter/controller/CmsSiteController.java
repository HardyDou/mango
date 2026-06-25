package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
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
import io.mango.cms.core.service.ICmsSiteService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.vo.FileDownloadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/cms-api")
@RequiredArgsConstructor
@ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "CMS 站点消费公共接口")
@Tag(name = "CMS 站点消费", description = "官网、帮助中心和门户站点只读接口")
public class CmsSiteController implements CmsSiteApi {

    private final ICmsSiteService service;

    @Override
    @GetMapping("/sites/resolve")
    @Operation(summary = "解析站点")
    public R<SiteResolveVO> resolveSite(@ParameterObject SiteResolveQuery query) {
        return service.resolveSite(query);
    }

    @Override
    @GetMapping("/sites/detail")
    @Operation(summary = "查询站点信息")
    public R<SiteVO> detailSite(@ParameterObject SiteResolveQuery query) {
        return service.detailSite(query);
    }

    @Override
    @GetMapping("/site-categories/tree")
    @Operation(summary = "查询公开栏目树")
    public R<List<SiteCategoryVO>> treeCategories(@ParameterObject SiteCategoryQuery query) {
        return service.treeCategories(query);
    }

    @Override
    @GetMapping("/navigations/list")
    @Operation(summary = "查询公开导航")
    public R<List<SiteNavigationVO>> listNavigations(@ParameterObject SiteNavigationQuery query) {
        return service.listNavigations(query);
    }

    @Override
    @GetMapping("/banners/list")
    @Operation(summary = "查询有效 Banner")
    public R<List<SiteBannerVO>> listBanners(@ParameterObject SiteBannerQuery query) {
        return service.listBanners(query);
    }

    @Override
    @GetMapping("/advertisements/list")
    @Operation(summary = "查询有效广告")
    public R<List<SiteAdvertisementVO>> listAdvertisements(@ParameterObject SiteAdvertisementQuery query) {
        return service.listAdvertisements(query);
    }

    @Override
    @GetMapping("/contents/page")
    @Operation(summary = "分页查询已发布内容")
    public R<PageResult<SiteContentVO>> pageContents(@ParameterObject SiteContentPageQuery query) {
        return service.pageContents(query);
    }

    @Override
    @GetMapping("/contents/detail")
    @Operation(summary = "查询已发布内容详情")
    public R<SiteContentVO> detailContent(@ParameterObject SiteContentDetailQuery query) {
        return service.detailContent(query);
    }

    @GetMapping("/files/public-preview")
    @Operation(summary = "读取公开站点素材")
    public ResponseEntity<InputStreamResource> publicFile(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id,
            @ParameterObject SiteResolveQuery query) {
        FileDownloadVO download = service.publicFile(id, query);
        String filename = UriUtils.encode(download.fileName(), StandardCharsets.UTF_8);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.contentType() != null && !download.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(download.contentType());
        }
        return ResponseEntity.ok()
                .contentLength(download.contentLength())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(download.inputStream()));
    }
}
