package io.mango.cms.starter.endpoint;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.query.SiteResolveQuery;
import io.mango.cms.core.service.ICmsSiteService;
import io.mango.common.contract.BinaryHttpAdapter;
import io.mango.file.api.vo.FileDownloadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/** CMS 公开文件二进制协议入口。 */
@BinaryHttpAdapter
@Validated
@RestController
@RequestMapping("/cms/open/files")
@RequiredArgsConstructor
@Tag(name = "CMS 公开文件", description = "官网和帮助中心公开文件预览接口")
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
    @Operation(summary = "读取 CMS 公开文件", description = "按站点编码或域名校验公开引用后以内联方式读取文件")
    public ResponseEntity<InputStreamResource> handle(
            @Parameter(description = "文件 ID", required = true)
            @RequestParam("id") @Pattern(regexp = "\\d+", message = "文件 ID 格式非法") String id,
            @Parameter(description = "站点编码")
            @RequestParam(value = "siteCode", required = false) @Size(max = 64, message = "站点编码最多64个字符") String siteCode,
            @Parameter(description = "站点域名")
            @RequestParam(value = "domain", required = false) @Size(max = 255, message = "站点域名最多255个字符") String domain) {
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
