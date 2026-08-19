package io.mango.auth.core.service.impl;

import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.core.service.IExternalIdentityAvatarService;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.file.api.FileApi;
import io.mango.file.api.FileImportApi;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIdentityAvatarService implements IExternalIdentityAvatarService {

    private static final String AVATAR_BIZ_TYPE = "identity-external-avatar";

    private final FileImportApi fileImportApi;
    private final FileApi fileApi;

    @Override
    public Long importAvatar(Long userId, String sourceUrl) {
        ImportRemoteImageCommand command = new ImportRemoteImageCommand();
        command.setSourceUrl(sourceUrl);
        command.setBizType(AVATAR_BIZ_TYPE);
        command.setBizId(String.valueOf(userId));
        R<FileRecordVO> response;
        try {
            response = fileImportApi.importImage(command);
        } catch (BizException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            return Require.fail(AuthCode.WECOM_PROFILE_SYNC_FAILED,
                    "企业微信头像导入失败，请稍后重试", exception);
        }
        Require.notNull(response, AuthCode.WECOM_PROFILE_SYNC_FAILED, "企业微信头像导入服务不可用");
        Require.isTrue(response.isSuccess(), response.getCode(), response.getMsg());
        FileRecordVO file = Require.nonNull(response.getData(), AuthCode.WECOM_PROFILE_SYNC_FAILED,
                "企业微信头像导入失败");
        return Require.nonNull(file.getId(), AuthCode.WECOM_PROFILE_SYNC_FAILED, "企业微信头像导入失败");
    }

    @Override
    public void deleteAvatar(Long fileId) {
        if (fileId == null) {
            return;
        }
        FileDeleteCommand command = new FileDeleteCommand();
        command.setIds(List.of(fileId));
        try {
            R<Boolean> response = fileApi.delete(command);
            if (response == null || !response.isSuccess() || !Boolean.TRUE.equals(response.getData())) {
                log.warn("企业微信头像文件清理失败: fileId={}", fileId);
            }
        } catch (RuntimeException exception) {
            log.warn("企业微信头像文件清理异常: fileId={}", fileId, exception);
        }
    }
}
