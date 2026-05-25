package io.mango.file.preview.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
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
import io.mango.infra.kv.api.ITokenStore;
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
import java.util.UUID;

/**
 * 文件预览服务实现。
 */
@Service
@RequiredArgsConstructor
public class FilePreviewServiceImpl implements IFilePreviewService {

    private static final String FULL_FILENAME_PARAM = "fullfilename";
    private static final String ENTRY_TOKEN_PREFIX = "file-preview:entry:";
    private static final String SOURCE_TOKEN_PREFIX = "file-preview:source:";

    private final FileApi fileApi;
    private final FilePreviewProperties properties;
    private final ITokenStore tokenStore;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public FilePreviewLinkVO createPreview(Long fileId) {
        Require.notNull(fileId, FilePreviewCode.FILE_ID_EMPTY);
        FileRecordVO fileRecord = fileRecord(fileId);
        String token = token();
        storeToken(ENTRY_TOKEN_PREFIX + token, new PreviewToken(fileId, MangoContextHolder.get(), expiresAt()));
        FilePreviewLinkVO vo = new FilePreviewLinkVO();
        vo.setFileId(fileId);
        vo.setFileName(fileRecord.getFileName());
        vo.setPreviewToken(token);
        vo.setPreviewUrl(previewEntryUrl(token));
        vo.setExpireSeconds(properties.getSourceTokenExpireSeconds());
        return vo;
    }

    @Override
    public FilePreviewLinkVO createEnginePreview(Long fileId) {
        Require.notNull(fileId, FilePreviewCode.FILE_ID_EMPTY);
        FileRecordVO fileRecord = fileRecord(fileId);
        String token = token();
        storeToken(SOURCE_TOKEN_PREFIX + token, new PreviewToken(fileId, MangoContextHolder.get(), expiresAt()));
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
        String tokenKey = SOURCE_TOKEN_PREFIX + token;
        PreviewToken sourceToken = readToken(tokenKey);
        if (isExpired(sourceToken)) {
            tokenStore.remove(tokenKey);
            throw tokenInvalid();
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

    public FilePreviewLinkVO createEnginePreviewByToken(String token) {
        PreviewToken previewToken = consumePreviewToken(token);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previewToken.context());
            return createEnginePreview(previewToken.fileId());
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

    private String previewEntryUrl(String token) {
        return UriComponentsBuilder.fromPath("/file-preview/files/preview-entry")
                .queryParam("token", token)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private PreviewToken consumePreviewToken(String token) {
        Require.notBlank(token, FilePreviewCode.PREVIEW_TOKEN_INVALID);
        String tokenKey = ENTRY_TOKEN_PREFIX + token;
        PreviewToken previewToken = readToken(tokenKey);
        tokenStore.remove(tokenKey);
        if (isExpired(previewToken)) {
            throw tokenInvalid();
        }
        return previewToken;
    }

    private void storeToken(String tokenKey, PreviewToken token) {
        try {
            StoredPreviewToken storedToken = new StoredPreviewToken(token.fileId(), StoredMangoContext.from(token.context()),
                    token.expiresAt().toEpochMilli());
            tokenStore.store(tokenKey, objectMapper.writeValueAsString(storedToken), properties.getSourceTokenExpireSeconds());
        } catch (JsonProcessingException e) {
            throw tokenInvalid(e);
        }
    }

    private PreviewToken readToken(String tokenKey) {
        String value = tokenStore.get(tokenKey);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            StoredPreviewToken storedToken = objectMapper.readValue(value, StoredPreviewToken.class);
            return new PreviewToken(storedToken.fileId(), storedToken.context().toSnapshot(),
                    Instant.ofEpochMilli(storedToken.expiresAtEpochMilli()));
        } catch (JsonProcessingException e) {
            throw tokenInvalid(e);
        }
    }

    private boolean isExpired(PreviewToken token) {
        return token == null || token.expiresAt() == null || token.expiresAt().isBefore(Instant.now(clock));
    }

    private BizException tokenInvalid() {
        return new BizException(FilePreviewCode.PREVIEW_TOKEN_INVALID.getCode(),
                FilePreviewCode.PREVIEW_TOKEN_INVALID.getMessage());
    }

    private BizException tokenInvalid(Throwable cause) {
        return new BizException(FilePreviewCode.PREVIEW_TOKEN_INVALID.getCode(),
                FilePreviewCode.PREVIEW_TOKEN_INVALID.getMessage(), cause);
    }

    private String token() {
        return UUID.randomUUID().toString().replace("-", "");
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

    private record PreviewToken(Long fileId, MangoContextSnapshot context, Instant expiresAt) {
    }

    private record StoredPreviewToken(Long fileId, StoredMangoContext context, long expiresAtEpochMilli) {
    }

    private record StoredMangoContext(
            String requestId,
            String traceId,
            String tenantId,
            Long userId,
            Long memberId,
            String principalName,
            String realm,
            String actorType,
            String partyType,
            Long partyId,
            String appCode,
            String clientIp
    ) {

        static StoredMangoContext from(MangoContextSnapshot context) {
            MangoContextSnapshot snapshot = context == null ? MangoContextSnapshot.empty() : context;
            return new StoredMangoContext(snapshot.requestId(),
                    snapshot.traceId(),
                    snapshot.tenantId(),
                    snapshot.userId(),
                    snapshot.memberId(),
                    snapshot.principalName(),
                    snapshot.realm(),
                    snapshot.actorType(),
                    snapshot.partyType(),
                    snapshot.partyId(),
                    snapshot.appCode(),
                    snapshot.clientIp());
        }

        MangoContextSnapshot toSnapshot() {
            return new MangoContextSnapshot(requestId,
                    traceId,
                    tenantId,
                    userId,
                    memberId,
                    principalName,
                    realm,
                    actorType,
                    partyType,
                    partyId,
                    appCode,
                    clientIp);
        }
    }
}
