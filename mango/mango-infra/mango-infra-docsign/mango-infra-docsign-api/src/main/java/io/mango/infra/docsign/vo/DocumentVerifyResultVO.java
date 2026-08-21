package io.mango.infra.docsign.vo;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.common.result.Require;
import io.mango.infra.docsign.enums.DocumentSignFormat;

import java.util.List;

/**
 * Document signature validation summary.
 */
@LocalCapabilityContract
public record DocumentVerifyResultVO(
        DocumentSignFormat format,
        boolean signed,
        boolean valid,
        List<SignatureValidationVO> signatures) {

    public DocumentVerifyResultVO {
        Require.notNull(format, "验签结果格式不能为空");
        signatures = signatures == null ? List.of() : List.copyOf(signatures);
    }
}
