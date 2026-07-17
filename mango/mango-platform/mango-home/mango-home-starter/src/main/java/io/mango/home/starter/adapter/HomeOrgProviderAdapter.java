package io.mango.home.starter.adapter;

import io.mango.common.result.R;
import io.mango.home.core.service.IHomeOrgProvider;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.vo.SysOrgVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** 将组织 API 适配为首页领域所需的纯组织层级能力。 */
@Component
@RequiredArgsConstructor
public class HomeOrgProviderAdapter implements IHomeOrgProvider {

    private final ObjectProvider<SysOrgApi> sysOrgApiProvider;

    @Override
    public Long findParentId(Long orgId) {
        SysOrgApi sysOrgApi = sysOrgApiProvider.getIfAvailable();
        if (sysOrgApi == null) {
            return null;
        }
        R<SysOrgVO> response = sysOrgApi.getById(orgId);
        if (response == null || response.getData() == null) {
            return null;
        }
        return response.getData().getPid();
    }
}
