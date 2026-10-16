package io.mango.cms.starter.endpoint;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.enums.CmsCode;
import io.mango.cms.api.query.SiteResolveQuery;
import io.mango.cms.core.service.ICmsSiteService;
import io.mango.common.result.Require;
import io.mango.file.api.vo.FileDownloadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/** CMS 公开文件二进制协议入口。 */
@RestController
@RequestMapping("/cms/open/files")
@RequiredArgsConstructor
public class CmsPublicFileEndpoint {

    private final ICmsSiteService siteService;

    /**
     * 读取已经通过 CMS 公开引用授权的文件。
     *
     * @param id 文件 ID
     * @param siteCode 站点编码
     * @param domain 站点域名
     * @return 二进制响应
     */
    @GetMapping("/public-preview")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "读取 CMS 公开文件")
    public ResponseEntity<InputStreamResource> handle(
            @RequestParam("id") String id,
            @RequestParam(value = "siteCode", required = false) String siteCode,
            @RequestParam(value = "domain", required = false) String domain) {
        Require.notBlank(id, CmsCode.CMS_BUSINESS_ERROR, "文件ID不能为空");
        Require.isTrue(id.matches("\\d+"), CmsCode.CMS_BUSINESS_ERROR, "文件ID格式非法");
        SiteResolveQuery query = new SiteResolveQuery();
        query.setSiteCode(siteCode);
        query.setDomain(domain);
        FileDownloadVO download = siteService.publicFile(Long.valueOf(id), query);
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
