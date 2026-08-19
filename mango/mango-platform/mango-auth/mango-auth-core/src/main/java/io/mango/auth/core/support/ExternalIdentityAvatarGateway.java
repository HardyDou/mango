package io.mango.auth.core.support;

import io.mango.auth.api.enums.AuthCode;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.file.api.FileApi;
import io.mango.file.api.FileImportApi;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;
import lombok.experimental.UtilityClass;

/**
 * 企业微信外部身份头像的文件中心调用适配器。
 */
@UtilityClass
public class ExternalIdentityAvatarGateway {

    public static Long importAvatar(FileImportApi fileImportApi, ImportRemoteImageCommand command) {
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
        Require.isTrue(response.isSuccess(), AuthCode.WECOM_PROFILE_SYNC_FAILED,
                response.getMsg() == null ? "企业微信头像导入失败" : response.getMsg());
        FileRecordVO file = Require.nonNull(response.getData(), AuthCode.WECOM_PROFILE_SYNC_FAILED,
                "企业微信头像导入失败");
        return Require.nonNull(file.getId(), AuthCode.WECOM_PROFILE_SYNC_FAILED, "企业微信头像导入失败");
    }

    public static boolean deleteAvatar(FileApi fileApi, FileDeleteCommand command) {
        R<Boolean> response = fileApi.delete(command);
        return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
    }
}
