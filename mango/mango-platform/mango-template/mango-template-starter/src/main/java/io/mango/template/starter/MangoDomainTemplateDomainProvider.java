package io.mango.template.starter;

import io.mango.common.result.R;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.vo.DomainVO;
import io.mango.template.core.service.ITemplateDomainProvider;
import io.mango.template.core.service.TemplateDomainInfo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 通过 Mango Domain API 为模板核心提供本地业务域视图。
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class MangoDomainTemplateDomainProvider implements ITemplateDomainProvider {

    private final DomainApi domainApi;

    @Override
    public TemplateDomainInfo findByCode(String domainCode) {
        R<DomainVO> response = domainApi.detailByCode(domainCode);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return null;
        }
        DomainVO domain = response.getData();
        return new TemplateDomainInfo(domain.getDomainCode(), domain.getDomainName(), domain.getStatus());
    }
}
