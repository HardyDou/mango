package io.mango.numgen.core.service.support;

import io.mango.common.result.R;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.vo.DomainVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 隔离业务域远程结果协议，为编号生成领域提供纯领域对象。
 */
@Component
@RequiredArgsConstructor
public class NumgenDomainSupport {

    private final DomainApi domainApi;

    /**
     * 按业务域编码查询启用状态等领域信息。
     *
     * @param domainCode 业务域编码
     * @return 查询成功时返回业务域，否则返回 {@code null}
     */
    public DomainVO getDomain(String domainCode) {
        if (!StringUtils.hasText(domainCode)) {
            return null;
        }
        R<DomainVO> response = domainApi.detailByCode(domainCode.trim());
        if (response == null || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }
}
