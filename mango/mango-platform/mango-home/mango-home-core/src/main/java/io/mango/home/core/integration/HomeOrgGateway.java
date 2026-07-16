package io.mango.home.core.integration;

import io.mango.common.result.R;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.vo.SysOrgVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 隔离首页领域与组织 API 的远程结果信封。
 */
@Component
@RequiredArgsConstructor
public class HomeOrgGateway {

    private final ObjectProvider<SysOrgApi> sysOrgApiProvider;

    /**
     * 查询组织；未装配组织能力或接口无数据时返回 {@code null}。
     *
     * @param orgId 组织 ID
     * @return 组织信息
     */
    public SysOrgVO findById(Long orgId) {
        SysOrgApi sysOrgApi = sysOrgApiProvider.getIfAvailable();
        if (sysOrgApi == null) {
            return null;
        }
        R<SysOrgVO> response = sysOrgApi.getById(orgId);
        return response == null ? null : response.getData();
    }
}
