package io.mango.information.starter.endpoint;

import io.mango.common.result.Require;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.information.api.enums.InformationCode;
import io.mango.information.api.vo.InformationFileVO;
import io.mango.information.api.vo.InformationUploadTicketVO;
import io.mango.information.core.service.impl.InformationFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 信息采集公开文件入口。
 * <p>
 * 托管表单是匿名页面，不能调用需要登录的文件接口；这里以表单为边界提供公开上传与预览，
 * 文件仍然通过 mango-file 保存和读取。
 */
@Component
@RequiredArgsConstructor
public class InformationPublicFileEndpoint {

    private final InformationFileService fileService;

    /** 匿名领取短效上传票据：票据绑定表单并带有效期。 */
    public ServerResponse ticket(ServerRequest request) {
        String formCode = formCode(request);
        InformationUploadTicketVO ticket = fileService.issueTicket(formCode);
        return ServerResponse.ok().body(ticket);
    }

    /** 匿名上传：必须携带有效票据，大小与类型限制由服务层按配置决定。 */
    public ServerResponse upload(ServerRequest request) throws IOException, ServletException {
        String formCode = formCode(request);
        String ticket = request.headers().firstHeader("X-Mango-Upload-Ticket");
        if (ticket == null) {
            ticket = request.param("ticket").orElse(null);
        }
        Part file = request.multipartData().getFirst("file");
        Require.notNull(file, InformationCode.INVALID_SUBMISSION, "上传文件不能为空");
        InformationFileVO result = fileService.upload(formCode, file.getSubmittedFileName(), file.getContentType(),
                file.getSize(), file.getInputStream(), ticket);
        return ServerResponse.ok().body(result);
    }

    /** 匿名查询文件信息，用于托管页面回显文件名与预览。 */
    public ServerResponse detail(ServerRequest request) {
        String formCode = formCode(request);
        Long id = fileId(request);
        return ServerResponse.ok().body(fileService.detail(formCode, id));
    }

    /** 匿名读取文件内容：必须携带当前表单签发的签名。 */
    public ServerResponse preview(ServerRequest request) {
        String formCode = formCode(request);
        Long id = fileId(request);
        FileDownloadVO download = fileService.download(formCode, id, request.param("sig").orElse(null));
        return ServerResponse.ok()
                .headers(target -> target.addAll(buildHeaders(download)))
                .body(new InputStreamResource(download.inputStream()));
    }

    private HttpHeaders buildHeaders(FileDownloadVO download) {
        HttpHeaders headers = new HttpHeaders();
        String fileName = download.fileName() == null ? "file" : download.fileName();
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        headers.setContentType(download.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.contentType()));
        headers.setContentLength(download.contentLength());
        return headers;
    }

    private String formCode(ServerRequest request) {
        String formCode = request.pathVariable("formCode");
        Require.isTrue(formCode != null && formCode.matches("[A-Za-z0-9_-]{1,64}"),
                InformationCode.FORM_NOT_FOUND, "表单编码格式非法");
        return formCode;
    }

    private Long fileId(ServerRequest request) {
        String raw = request.param("id").orElse(null);
        Require.isTrue(raw != null && raw.matches("\\d+"), InformationCode.INVALID_SUBMISSION, "文件ID格式非法");
        return Long.valueOf(raw);
    }
}
