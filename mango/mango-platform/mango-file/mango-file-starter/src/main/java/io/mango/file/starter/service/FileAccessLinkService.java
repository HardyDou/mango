package io.mango.file.starter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.file.api.FileCode;
import io.mango.file.api.enums.FileAccessAction;
import io.mango.file.api.vo.FileAccessLinkVO;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IFileService;
import io.mango.file.core.service.IFileSettingsService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ITokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class FileAccessLinkService {
    private static final String PREFIX = "file:access:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IFileService fileService;
    private final IFileSettingsService settingsService;
    private final ITokenStore tokenStore;
    private final ObjectMapper objectMapper;

    public FileAccessLinkVO create(Long fileId, FileAccessAction action) {
        R<FileRecordVO> result = fileService.get(fileId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BizException(FileCode.FILE_NOT_FOUND.getCode(), FileCode.FILE_NOT_FOUND.getMessage());
        }
        long ttl = settingsService.current().getAccessTokenExpireSeconds();
        String token = token();
        Grant grant = new Grant(fileId, action, MangoContextHolder.get().tenantId(),
                Instant.now().plusSeconds(ttl).toEpochMilli());
        try {
            tokenStore.store(PREFIX + token, objectMapper.writeValueAsString(grant), ttl);
        } catch (JsonProcessingException e) {
            throw invalid(e);
        }
        FileAccessLinkVO link = new FileAccessLinkVO();
        link.setAction(action);
        link.setExpireSeconds(ttl);
        link.setUrl(UriComponentsBuilder.fromPath("/file/files/access")
                .queryParam("token", token).build().encode().toUriString());
        return link;
    }

    public AccessContent open(String token) {
        Grant grant;
        try {
            String value = tokenStore.get(PREFIX + token);
            grant = value == null ? null : objectMapper.readValue(value, Grant.class);
        } catch (Exception e) {
            throw invalid(e);
        }
        if (grant == null || grant.expiresAt() < System.currentTimeMillis()) {
            throw invalid(null);
        }
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId(grant.tenantId()));
            FileDownloadVO download = fileService.downloadForService(grant.fileId());
            return new AccessContent(download, grant.action());
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private BizException invalid(Throwable cause) {
        return new BizException(FileCode.FILE_ACCESS_DENIED.getCode(), "文件访问链接无效或已过期", cause);
    }

    private record Grant(Long fileId, FileAccessAction action, String tenantId, long expiresAt) {}
    public record AccessContent(FileDownloadVO download, FileAccessAction action) {}
}
