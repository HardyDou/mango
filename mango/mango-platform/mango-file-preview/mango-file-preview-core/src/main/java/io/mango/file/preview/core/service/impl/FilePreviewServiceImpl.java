package io.mango.file.preview.core.service.impl;

import io.mango.common.result.Require;
import io.mango.common.result.R;
import io.mango.file.api.FileApi;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.preview.api.FilePreviewCode;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件预览服务实现。
 */
@Service
@RequiredArgsConstructor
public class FilePreviewServiceImpl implements IFilePreviewService {

    private static final String FULL_FILENAME_PARAM = "fullfilename";

    private final FileApi fileApi;
    private final FilePreviewProperties properties;
    private final Clock clock;
    private final Map<String, SourceToken> sourceTokens = new ConcurrentHashMap<>();

    @Override
    public FilePreviewLinkVO createPreview(Long fileId) {
        Require.notNull(fileId, FilePreviewCode.FILE_ID_EMPTY);
        FileRecordVO fileRecord = fileRecord(fileId);
        FilePreviewLinkVO vo = new FilePreviewLinkVO();
        vo.setFileId(fileId);
        vo.setFileName(fileRecord.getFileName());
        vo.setPreviewUrl(previewEntryUrl(fileId));
        vo.setExpireSeconds(properties.getSourceTokenExpireSeconds());
        return vo;
    }

    @Override
    public FilePreviewLinkVO createEnginePreview(Long fileId) {
        Require.notNull(fileId, FilePreviewCode.FILE_ID_EMPTY);
        FileRecordVO fileRecord = fileRecord(fileId);
        String token = UUID.randomUUID().toString().replace("-", "");
        sourceTokens.put(token, new SourceToken(fileId, MangoContextHolder.get(), expiresAt()));
        String sourceUrl = sourceUrl(token, fileRecord.getFileName());
        String encodedSourceUrl = Base64.getEncoder().encodeToString(sourceUrl.getBytes(StandardCharsets.UTF_8));
        FilePreviewLinkVO vo = new FilePreviewLinkVO();
        vo.setFileId(fileId);
        vo.setFileName(fileRecord.getFileName());
        vo.setPreviewUrl(engineUrl(encodedSourceUrl));
        vo.setExpireSeconds(properties.getSourceTokenExpireSeconds());
        return vo;
    }

    @Override
    public FilePreviewSource openSource(String token) {
        Require.notBlank(token, FilePreviewCode.PREVIEW_TOKEN_INVALID);
        SourceToken sourceToken = sourceTokens.get(token);
        if (sourceToken == null || sourceToken.expiresAt().isBefore(Instant.now(clock))) {
            sourceTokens.remove(token);
            throw new io.mango.common.exception.BizException(FilePreviewCode.PREVIEW_TOKEN_INVALID.getCode(),
                    FilePreviewCode.PREVIEW_TOKEN_INVALID.getMessage());
        }
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(sourceToken.context());
            FileDownloadVO download = fileApi.download(sourceToken.fileId());
            return new FilePreviewSource(download.inputStream(), download.fileName(), download.contentType(), download.contentLength());
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private Instant expiresAt() {
        return Instant.now(clock).plusSeconds(properties.getSourceTokenExpireSeconds());
    }

    private FileRecordVO fileRecord(Long fileId) {
        R<FileRecordVO> result = fileApi.get(fileId);
        Require.isTrue(result != null && result.isSuccess() && result.getData() != null,
                FilePreviewCode.FILE_NOT_FOUND);
        return result.getData();
    }

    private String sourceUrl(String token, String fileName) {
        String url = currentBaseUrl() + normalize(properties.getSourcePath()) + "/" + token;
        return UriComponentsBuilder.fromHttpUrl(url)
                .queryParam(FULL_FILENAME_PARAM, fileName)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private String engineUrl(String encodedSourceUrl) {
        return UriComponentsBuilder.fromPath(normalize(properties.getEnginePath()))
                .queryParam("url", encodedSourceUrl)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private String previewEntryUrl(Long fileId) {
        return UriComponentsBuilder.fromPath("/file-preview/files/preview")
                .queryParam("fileId", fileId)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private String currentBaseUrl() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "http://127.0.0.1";
        }
        HttpServletRequest request = attributes.getRequest();
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port > 0 && port != 80 && port != 443) {
            builder.append(':').append(port);
        }
        if (StringUtils.hasText(request.getContextPath())) {
            builder.append(request.getContextPath());
        }
        return builder.toString();
    }

    private String normalize(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private record SourceToken(Long fileId, MangoContextSnapshot context, Instant expiresAt) {
    }
}
