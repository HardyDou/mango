package io.mango.file.starter.remote;

import io.mango.common.exception.BizException;
import io.mango.file.api.FileCode;
import io.mango.file.api.vo.FileDownloadVO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * 远程下载响应转换。
 */
final class FileRemoteDownloadConverter {

    private FileRemoteDownloadConverter() {
    }

    static FileDownloadVO toFileDownload(ResponseEntity<byte[]> response) {
        if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BizException(FileCode.FILE_READ_FAILED.getCode(), FileCode.FILE_READ_FAILED.getMessage());
        }
        byte[] body = response.getBody();
        return new FileDownloadVO(new ByteArrayInputStream(body), fileName(response), contentType(response), body.length);
    }

    private static String fileName(ResponseEntity<byte[]> response) {
        ContentDisposition disposition = response.getHeaders().getContentDisposition();
        String fileName = disposition.getFilename();
        if (fileName == null || fileName.isBlank()) {
            fileName = "download";
        }
        return fileName;
    }

    private static String contentType(ResponseEntity<byte[]> response) {
        MediaType mediaType = response.getHeaders().getContentType();
        if (mediaType != null) {
            return mediaType.toString();
        }
        String header = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return header == null || header.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : new String(header.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }
}
