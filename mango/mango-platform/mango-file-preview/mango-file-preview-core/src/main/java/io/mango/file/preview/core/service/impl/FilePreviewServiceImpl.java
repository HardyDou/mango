package io.mango.file.preview.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.result.Require;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.preview.api.enums.FilePreviewCode;
import io.mango.file.preview.api.enums.FilePreviewRenderMode;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.gateway.FilePreviewFileGateway;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.PreviewSourceAvailability;
import io.mango.file.preview.core.task.IFilePreviewTaskService;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;
import io.mango.file.preview.core.service.model.FilePreviewMode;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ITokenStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件预览服务实现。
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring collaborators are intentionally retained for the service lifetime"))
public class FilePreviewServiceImpl implements IFilePreviewService {
    private static final int SOURCE_VERSION_HEX_LENGTH = 16;
    private static final int SOURCE_VERSION_DIGEST_BYTES = 8;
    private static final long HASH_FALLBACK_MASK = 0xffffffffL;

    private static final String FULL_FILENAME_PARAM = "fullfilename";
    /** 内部直取协议；与引擎 {@code cn.keking.utils.PreviewSourceUrl} 保持一致，跨模块由 starter 的往返测试锁定。 */
    private static final String SOURCE_ID_PARAM = "fileId";
    private static final String ENTRY_TOKEN_PREFIX = "file-preview:entry:";
    private static final String GATEWAY_PATH_PREFIX = "/api";
    private static final String SOURCE_TOKEN_PREFIX = "file-preview:source:";
    private static final String ENGINE_FILE_PREFIX = "file-";
    private static final int MAX_EXTENSION_LENGTH = 20;
    /**
     * 图片扩展名；与引擎 {@code cn.keking.model.FileType#PICTURE_TYPES} 保持一致，跨模块由引擎测试锁定。
     */
    private static final Set<String> PICTURE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "ico", "jfif", "webp", "heic", "avif", "heif");
    /**
     * 视频扩展名；取自引擎 {@code cn.keking.config.ConfigConstants#DEFAULT_MEDIA_TYPE}（{@code mp3,wav,mp4,flv}）。
     */
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "flv");
    /**
     * 音频扩展名；取自引擎 {@code cn.keking.config.ConfigConstants#DEFAULT_MEDIA_TYPE}（{@code mp3,wav,mp4,flv}）。
     */
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav");
    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewServiceImpl.class);

    private final FilePreviewFileGateway fileGateway;
    private final FilePreviewProperties properties;
    private final ITokenStore tokenStore;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final IFilePreviewTaskService previewTaskService;
    private final ObjectProvider<PreviewSourceAvailability> sourceAvailabilityProvider;

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
        vo.setRenderMode(renderMode(fileRecord));
        vo.setExpireSeconds(properties.getSourceTokenExpireSeconds());
        return vo;
    }

    @Override
    public FilePreviewLinkVO createEnginePreview(Long fileId) {
        Require.notNull(fileId, FilePreviewCode.FILE_ID_EMPTY);
        FileRecordVO fileRecord = fileRecord(fileId);
        String token = token();
        storeToken(SOURCE_TOKEN_PREFIX + token, new PreviewToken(fileId, MangoContextHolder.get(), expiresAt()));
        FilePreviewLinkVO vo = new FilePreviewLinkVO();
        vo.setFileId(fileId);
        vo.setFileName(fileRecord.getFileName());
        vo.setPreviewUrl(enginePreviewUrl(token, engineFileName(fileId, fileRecord)));
        vo.setRenderMode(renderMode(fileRecord));
        vo.setExpireSeconds(properties.getSourceTokenExpireSeconds());
        return vo;
    }

    @Override
    public FilePreviewSource openSource(String token) {
        PreviewToken sourceToken = readSourceToken(token);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(sourceToken.context());
            FileDownloadVO download = fileGateway.download(sourceToken.fileId());
            Require.notNull(download, FilePreviewCode.FILE_NOT_FOUND);
            return new FilePreviewSource(download.inputStream(), download.fileName(), download.contentType(), download.contentLength());
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public boolean isSourceTokenValid(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            PreviewToken sourceToken = readToken(SOURCE_TOKEN_PREFIX + token);
            return !isExpired(sourceToken);
        } catch (RuntimeException e) {
            LOGGER.warn("预览取源令牌校验失败，按不可用处理：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public void validateGeneratedAccess(String token, String fileName) {
        Require.notBlank(fileName, FilePreviewCode.PREVIEW_TOKEN_INVALID);
        PreviewToken sourceToken = readSourceToken(token);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(sourceToken.context());
            FileRecordVO fileRecord = fileRecord(sourceToken.fileId());
            Require.isTrue(fileName.equals(generatedPdfFileName(sourceToken.fileId(), fileRecord)),
                    FilePreviewCode.PREVIEW_TOKEN_INVALID);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    public FilePreviewLinkVO createEnginePreviewByToken(String token) {
        Require.notBlank(token, FilePreviewCode.PREVIEW_TOKEN_INVALID);
        PreviewToken previewToken = readPreviewToken(token);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previewToken.context());
            return createEnginePreview(previewToken.fileId());
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public Long resolvePreviewFileId(String token) {
        return readPreviewToken(token).fileId();
    }

    @Override
    public FilePreviewTaskVO previewTaskByToken(String token, boolean allowLargeFile) {
        PreviewToken previewToken = readPreviewToken(token);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previewToken.context());
            return previewTaskService.status(previewToken.fileId(), allowLargeFile);
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public FileDownloadVO downloadPreviewArtifact(String token) {
        PreviewToken previewToken = readPreviewToken(token);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previewToken.context());
            FilePreviewTaskVO task = previewTaskService.status(previewToken.fileId(), false);
            Require.isTrue(task.getStatus() == io.mango.file.preview.api.enums.FilePreviewTaskStatus.SUCCEEDED
                    && task.getPreviewFileId() != null, FilePreviewCode.PREVIEW_TOKEN_INVALID);
            FileDownloadVO download = fileGateway.download(task.getPreviewFileId());
            Require.notNull(download, FilePreviewCode.FILE_NOT_FOUND);
            return download;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public FileDownloadVO downloadPreviewSource(String token) {
        PreviewToken previewToken = readPreviewToken(token);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previewToken.context());
            FileDownloadVO download = fileGateway.download(previewToken.fileId());
            Require.notNull(download, FilePreviewCode.FILE_NOT_FOUND);
            return download;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public FilePreviewMode previewMode(String token) {
        PreviewToken previewToken = readPreviewToken(token);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previewToken.context());
            return previewMode(fileRecord(previewToken.fileId()));
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private FilePreviewMode previewMode(FileRecordVO record) {
        String extension = FilePreviewMode.extension(record.getFileExt(), record.getFileName());
        FilePreviewMode mode = FilePreviewMode.of(extension, useInternalSource());
        if (mode == FilePreviewMode.NATIVE_PDF) {
            return mode;
        }
        // 扩展名缺失或与实际内容不一致时，按声明的 PDF 内容类型兜底。
        return "application/pdf".equalsIgnoreCase(record.getContentType())
                ? FilePreviewMode.NATIVE_PDF
                : mode;
    }

    /**
     * 把文件扩展名、预览模式与可预览性映射为前端渲染类型，格式判定只在这里发生。
     *
     * <p>可预览性指能否解析出文件扩展名：引擎按扩展名分派渲染器，扩展名缺失时统一落到不支持页，
     * 前端应直接引导下载。</p>
     */
    private FilePreviewRenderMode renderMode(String extension, FilePreviewMode mode, boolean previewable) {
        if (mode == FilePreviewMode.NATIVE_PDF) {
            return FilePreviewRenderMode.PDF;
        }
        if (mode == FilePreviewMode.PDF_CONVERSION) {
            return FilePreviewRenderMode.CONVERTING;
        }
        if (!previewable) {
            return FilePreviewRenderMode.NOT_SUPPORTED;
        }
        if (PICTURE_EXTENSIONS.contains(extension)) {
            return FilePreviewRenderMode.IMAGE;
        }
        if (VIDEO_EXTENSIONS.contains(extension)) {
            return FilePreviewRenderMode.VIDEO;
        }
        if (AUDIO_EXTENSIONS.contains(extension)) {
            return FilePreviewRenderMode.AUDIO;
        }
        return FilePreviewRenderMode.PAGE;
    }

    private FilePreviewRenderMode renderMode(FileRecordVO record) {
        String extension = FilePreviewMode.extension(record.getFileExt(), record.getFileName());
        return renderMode(extension, previewMode(record), StringUtils.hasText(extension));
    }

    private Instant expiresAt() {
        return Instant.now(clock).plusSeconds(properties.getSourceTokenExpireSeconds());
    }

    private FileRecordVO fileRecord(Long fileId) {
        FileRecordVO fileRecord = fileGateway.find(fileId);
        Require.notNull(fileRecord, FilePreviewCode.FILE_NOT_FOUND);
        return fileRecord;
    }

    /**
     * 引擎预览地址，按通道优先级选择参数：
     *
     * <ol>
     *   <li>主路径 {@code fileId}：本次部署注册了取源实现（内嵌部署是同进程直取，独立部署的引擎是自己进程内的
     *       远程直取），传短期取源票据，引擎按该标识取流，不暴露对外域名；</li>
     *   <li>兼容兜底 {@code url}：只有 {@code internal-source-enabled=false}（运维强制走兜底）或没有任何
     *       取源实现时才走，且必须显式配置 {@code source-base-url}，否则直接报错。</li>
     * </ol>
     */
    private String enginePreviewUrl(String sourceTicket, String fileName) {
        if (!useInternalSource()) {
            warnIfInternalSourceUnavailable();
            String sourceUrl = sourceUrl(sourceTicket, fileName);
            return engineUrl(Base64.getEncoder().encodeToString(sourceUrl.getBytes(StandardCharsets.UTF_8)));
        }
        String relative = UriComponentsBuilder.fromPath(enginePathPrefix() + normalize(properties.getEnginePath()))
                .queryParam(SOURCE_ID_PARAM, sourceTicket)
                .queryParam(FULL_FILENAME_PARAM, fileName)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        return withEngineOrigin(relative);
    }

    private String sourceUrl(String token, String fileName) {
        String url = sourceBaseUrl() + normalize(properties.getSourcePath());
        return UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("token", token)
                .queryParam(FULL_FILENAME_PARAM, fileName)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private boolean useInternalSource() {
        if (!properties.isInternalSourceEnabled()) {
            return false;
        }
        PreviewSourceAvailability availability = sourceAvailabilityProvider.getIfAvailable();
        return availability != null && availability.internalSourceAvailable();
    }

    /**
     * 只有「期望直取、但进程里没有可用实现」才告警：独立引擎部署显式关掉开关时回落属于预期行为。
     */
    private void warnIfInternalSourceUnavailable() {
        if (!properties.isInternalSourceEnabled()) {
            return;
        }
        PreviewSourceAvailability availability = sourceAvailabilityProvider.getIfAvailable();
        if (availability == null || !availability.internalSourceAvailable()) {
            LOGGER.warn("未装配 FileDownloader 直取实现（内嵌直取或远程直取），预览源文件回落到 url 下载通道，"
                    + "取源地址基于 mango.file-preview.source-base-url：{}", properties.getSourceBaseUrl());
        }
    }

    /**
     * url 兜底通道使用的内部服务地址。
     *
     * <p>{@code fileId} 是取源主路径：内嵌部署由进程内直取，独立部署的引擎由远程直取，都不需要本配置。
     * 只有直取实现不可用时才回落到 {@code url} 通道，此时必须显式配置 {@code source-base-url}：
     * 不再从当前请求推断，避免在多实例、网关和反向代理下静默拼出一个不可达或绕公网的地址。</p>
     */
    private String sourceBaseUrl() {
        Require.notBlank(properties.getSourceBaseUrl(), FilePreviewCode.SOURCE_BASE_URL_MISSING);
        return StringUtils.trimTrailingCharacter(properties.getSourceBaseUrl().trim(), '/');
    }

    private String engineFileName(Long fileId, FileRecordVO fileRecord) {
        String originalFileName = fileRecord.getFileName();
        String baseName = ENGINE_FILE_PREFIX + fileId;
        baseName += "-" + sourceVersionKey(fileRecord);
        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (!isSafeExtension(extension)) {
            return baseName;
        }
        return baseName + "." + extension.toLowerCase(Locale.ROOT);
    }

    private String generatedPdfFileName(Long fileId, FileRecordVO fileRecord) {
        String engineFileName = engineFileName(fileId, fileRecord);
        int extensionSeparator = engineFileName.lastIndexOf('.');
        Require.isTrue(extensionSeparator > 0, FilePreviewCode.PREVIEW_TOKEN_INVALID);
        String baseName = engineFileName.substring(0, extensionSeparator);
        String extension = engineFileName.substring(extensionSeparator + 1);
        return baseName + extension + ".pdf";
    }

    private String sourceVersionKey(FileRecordVO fileRecord) {
        String source = String.join("|",
                String.valueOf(fileRecord.getId()),
                String.valueOf(fileRecord.getFileHash()),
                String.valueOf(fileRecord.getFileSize()),
                String.valueOf(fileRecord.getUpdatedTime()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(SOURCE_VERSION_HEX_LENGTH);
            for (int index = 0; index < SOURCE_VERSION_DIGEST_BYTES; index++) {
                value.append(String.format(Locale.ROOT, "%02x", digest[index]));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) {
            return String.format(Locale.ROOT, "%016x", source.hashCode() & HASH_FALLBACK_MASK);
        }
    }

    private boolean isSafeExtension(String extension) {
        if (!StringUtils.hasText(extension) || extension.length() > MAX_EXTENSION_LENGTH) {
            return false;
        }
        for (int index = 0; index < extension.length(); index++) {
            char current = extension.charAt(index);
            if (!isAsciiLetterOrDigit(current)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAsciiLetterOrDigit(char value) {
        return isBetween(value, 'a', 'z') || isBetween(value, 'A', 'Z') || isBetween(value, '0', '9');
    }

    private boolean isBetween(char value, char start, char end) {
        return value >= start && value <= end;
    }

    private String engineUrl(String encodedSourceUrl) {
        String relative = UriComponentsBuilder.fromPath(enginePathPrefix() + normalize(properties.getEnginePath()))
                .queryParam("url", encodedSourceUrl)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        return withEngineOrigin(relative);
    }

    /**
     * 引擎地址的前缀：内嵌部署走应用网关，需要 {@code /api} 前缀；独立部署（配置了
     * {@link FilePreviewProperties#getEnginePublicBaseUrl()}）时浏览器直连引擎，不能再带网关前缀。
     *
     * <p>这里只决定前缀，不改 {@code engine-path}：后者是可配置项，替换会误伤自定义路径。</p>
     */
    private String enginePathPrefix() {
        String baseUrl = properties.getEnginePublicBaseUrl();
        return StringUtils.hasText(baseUrl) ? "" : GATEWAY_PATH_PREFIX;
    }

    /**
     * 给引擎地址补上对外 origin。
     *
     * <p>独立部署时引擎不在本进程，返回相对地址会让浏览器/应用自己去找引擎资源（应用里并没有这些资源），
     * 因此必须给出带 origin 的绝对地址；未配置时保持相对地址，等价于内嵌部署。</p>
     */
    private String withEngineOrigin(String relative) {
        String base = properties.getEnginePublicBaseUrl();
        if (!StringUtils.hasText(base)) {
            return relative;
        }
        String trimmed = base.trim();
        String origin = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        return origin + relative;
    }

    private String previewEntryUrl(String token) {
        return UriComponentsBuilder.fromPath("/api/file-preview/files/preview-entry")
                .queryParam("token", token)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private PreviewToken readPreviewToken(String token) {
        Require.notBlank(token, FilePreviewCode.PREVIEW_TOKEN_INVALID);
        String tokenKey = ENTRY_TOKEN_PREFIX + token;
        PreviewToken previewToken = readToken(tokenKey);
        if (isExpired(previewToken)) {
            tokenStore.remove(tokenKey);
            Require.fail(FilePreviewCode.PREVIEW_TOKEN_INVALID);
        }
        return previewToken;
    }

    private PreviewToken readSourceToken(String token) {
        Require.notBlank(token, FilePreviewCode.PREVIEW_TOKEN_INVALID);
        String tokenKey = SOURCE_TOKEN_PREFIX + token;
        PreviewToken sourceToken = readToken(tokenKey);
        if (isExpired(sourceToken)) {
            tokenStore.remove(tokenKey);
            Require.fail(FilePreviewCode.PREVIEW_TOKEN_INVALID);
        }
        return sourceToken;
    }

    private void storeToken(String tokenKey, PreviewToken token) {
        try {
            StoredPreviewToken storedToken = new StoredPreviewToken(token.fileId(), StoredMangoContext.from(token.context()),
                    token.expiresAt().toEpochMilli());
            tokenStore.store(tokenKey, objectMapper.writeValueAsString(storedToken), properties.getSourceTokenExpireSeconds());
        } catch (JsonProcessingException e) {
            Require.fail(FilePreviewCode.PREVIEW_TOKEN_INVALID,
                    FilePreviewCode.PREVIEW_TOKEN_INVALID.getMessage(), e);
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
            return Require.fail(FilePreviewCode.PREVIEW_TOKEN_INVALID,
                    FilePreviewCode.PREVIEW_TOKEN_INVALID.getMessage(), e);
        }
    }

    private boolean isExpired(PreviewToken token) {
        return token == null || token.expiresAt() == null || token.expiresAt().isBefore(Instant.now(clock));
    }

    private String token() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String normalize(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        if (path.startsWith("/")) {
            return path;
        }
        return "/" + path;
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
            MangoContextSnapshot snapshot = context;
            if (snapshot == null) {
                snapshot = MangoContextSnapshot.empty();
            }
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
