package io.mango.cms.starter.endpoint;

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
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/** CMS 公开文件二进制协议入口。 */
@Component
@RequiredArgsConstructor
public class CmsPublicFileEndpoint {

    private final ICmsSiteService siteService;

    /**
     * 读取已经通过 CMS 公开引用授权的文件。
     *
     * @param request HTTP 请求
     * @return 二进制响应
     */
    public ServerResponse handle(ServerRequest request) {
        String rawId = request.param("id").orElse(null);
        Require.notBlank(rawId, CmsCode.CMS_BUSINESS_ERROR, "文件ID不能为空");
        Require.isTrue(rawId.matches("\\d+"), CmsCode.CMS_BUSINESS_ERROR, "文件ID格式非法");
        SiteResolveQuery query = new SiteResolveQuery();
        query.setSiteCode(request.param("siteCode").orElse(null));
        query.setDomain(request.param("domain").orElse(null));
        FileDownloadVO download = siteService.publicFile(Long.valueOf(rawId), query);
        String filename = UriUtils.encode(download.fileName(), StandardCharsets.UTF_8);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.contentType() != null && !download.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(download.contentType());
        }
        return ServerResponse.ok()
                .contentLength(download.contentLength())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(download.inputStream()));
    }
}
