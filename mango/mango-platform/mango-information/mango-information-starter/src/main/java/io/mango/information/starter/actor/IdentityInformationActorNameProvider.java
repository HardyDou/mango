package io.mango.information.starter.actor;

import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.request.IdentityUserBatchRequest;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.information.core.service.InformationActorNameProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * 基于平台身份能力的人员显示名适配。
 * <p>
 * 只用于界面展示；查询失败时返回空映射，列表和详情回退展示兜底文案，不阻断主流程。
 */
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring 容器管理的单例依赖，构造后不对外暴露"))
public class IdentityInformationActorNameProvider implements InformationActorNameProvider {

    private static final int MAX_BATCH = IdentityUserBatchRequest.MAX_IDENTIFIERS;

    private final IdentityUserApi identityUserApi;

    @Override
    public Map<Long, String> resolveNames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty() || identityUserApi == null) {
            return Map.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        List<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        for (int index = 0; index < ids.size(); index += MAX_BATCH) {
            List<Long> batch = ids.subList(index, Math.min(index + MAX_BATCH, ids.size()));
            IdentityUserBatchRequest request = new IdentityUserBatchRequest();
            request.setUserIds(batch);
            R<List<IdentityUserInfoVO>> response;
            try {
                response = identityUserApi.listUserInfos(request);
            } catch (RuntimeException e) {
                return names;
            }
            if (response == null || response.getData() == null) {
                continue;
            }
            for (IdentityUserInfoVO user : response.getData()) {
                if (user == null || user.getUserId() == null) {
                    continue;
                }
                String displayName = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
                if (StringUtils.hasText(displayName)) {
                    names.put(user.getUserId(), displayName);
                }
            }
        }
        return names;
    }
}
