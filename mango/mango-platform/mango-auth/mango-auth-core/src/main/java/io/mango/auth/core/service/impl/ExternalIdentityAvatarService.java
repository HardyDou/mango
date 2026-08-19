package io.mango.auth.core.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.core.service.IExternalIdentityAvatarService;
import io.mango.auth.core.support.ExternalIdentityAvatarGateway;
import io.mango.common.result.Require;
import io.mango.file.api.FileApi;
import io.mango.file.api.FileImportApi;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.ImportRemoteImageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-managed file API collaborators are intentionally shared")
public class ExternalIdentityAvatarService implements IExternalIdentityAvatarService {

    private static final String AVATAR_BIZ_TYPE = "identity-external-avatar";

    private final FileImportApi fileImportApi;
    private final FileApi fileApi;

    @Override
    public Long importAvatar(Long userId, String sourceUrl) {
        Require.notNull(userId, AuthCode.WECOM_PROFILE_SYNC_FAILED, "企业微信头像缺少用户标识");
        Require.notBlank(sourceUrl, AuthCode.WECOM_PROFILE_SYNC_FAILED, "企业微信头像地址为空");
        ImportRemoteImageCommand command = new ImportRemoteImageCommand();
        command.setSourceUrl(sourceUrl);
        command.setBizType(AVATAR_BIZ_TYPE);
        command.setBizId(String.valueOf(userId));
        return ExternalIdentityAvatarGateway.importAvatar(fileImportApi, command);
    }

    @Override
    public void deleteAvatar(Long fileId) {
        if (fileId == null) {
            return;
        }
        Require.notNull(fileId, AuthCode.WECOM_PROFILE_SYNC_FAILED, "企业微信头像文件标识为空");
        FileDeleteCommand command = new FileDeleteCommand();
        command.setIds(List.of(fileId));
        try {
            if (!ExternalIdentityAvatarGateway.deleteAvatar(fileApi, command)) {
                log.warn("企业微信头像文件清理失败: fileId={}", fileId);
            }
        } catch (RuntimeException exception) {
            log.warn("企业微信头像文件清理异常: fileId={}", fileId, exception);
        }
    }
}
