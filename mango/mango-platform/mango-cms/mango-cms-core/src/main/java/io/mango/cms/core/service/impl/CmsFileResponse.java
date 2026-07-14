package io.mango.cms.core.service.impl;

import io.mango.cms.api.enums.CmsCode;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.file.api.vo.FileRecordVO;

/**
 * CMS 文件服务响应适配器。
 *
 * <p>把远程协议结果解包限制在领域服务之外，领域服务只消费文件记录。</p>
 */
final class CmsFileResponse {

    private CmsFileResponse() {
    }

    static FileRecordVO requireRecord(R<FileRecordVO> response, String fieldName) {
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null,
                CmsCode.CMS_BUSINESS_ERROR, fieldName + "不存在或不可见");
        return response.getData();
    }
}
