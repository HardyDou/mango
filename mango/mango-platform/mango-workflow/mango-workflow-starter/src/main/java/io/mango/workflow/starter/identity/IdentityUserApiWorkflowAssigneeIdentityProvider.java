package io.mango.workflow.starter.identity;

import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.request.IdentityUserBatchRequest;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.workflow.core.identity.IWorkflowAssigneeIdentityProvider;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过 Mango Identity 公共 API 解析 Workflow 办理人身份。
 */
@Component
@RequiredArgsConstructor
public class IdentityUserApiWorkflowAssigneeIdentityProvider implements IWorkflowAssigneeIdentityProvider {

    private final ObjectProvider<IdentityUserApi> identityUserApiProvider;

    @Override
    public Map<String, WorkflowAssigneeIdentity> resolveAll(Collection<String> assigneeKeys) {
        IdentityUserApi identityUserApi = identityUserApiProvider.getIfAvailable();
        if (identityUserApi == null || assigneeKeys == null || assigneeKeys.isEmpty()) {
            return Map.of();
        }
        R<List<IdentityUserInfoVO>> response = identityUserApi.listUserInfos(buildRequest(assigneeKeys));
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return Map.of();
        }
        Map<Long, IdentityUserInfoVO> byId = new LinkedHashMap<>();
        Map<String, IdentityUserInfoVO> byUsername = new LinkedHashMap<>();
        indexUsers(response.getData(), byId, byUsername);
        return resolveIdentities(assigneeKeys, byId, byUsername);
    }

    private IdentityUserBatchRequest buildRequest(Collection<String> assigneeKeys) {
        List<Long> userIds = new ArrayList<>();
        List<String> usernames = new ArrayList<>();
        for (String assigneeKey : assigneeKeys) {
            if (!StringUtils.hasText(assigneeKey)) {
                continue;
            }
            Long userId = parseLong(assigneeKey.trim());
            if (userId == null) {
                usernames.add(assigneeKey.trim());
            } else {
                userIds.add(userId);
            }
        }
        IdentityUserBatchRequest request = new IdentityUserBatchRequest();
        request.setUserIds(userIds);
        request.setUsernames(usernames);
        return request;
    }

    private void indexUsers(List<IdentityUserInfoVO> users,
                            Map<Long, IdentityUserInfoVO> byId,
                            Map<String, IdentityUserInfoVO> byUsername) {
        for (IdentityUserInfoVO user : users) {
            if (user == null) {
                continue;
            }
            if (user.getUserId() != null) {
                byId.putIfAbsent(user.getUserId(), user);
            }
            if (StringUtils.hasText(user.getUsername())) {
                byUsername.putIfAbsent(user.getUsername(), user);
            }
        }
    }

    private Map<String, WorkflowAssigneeIdentity> resolveIdentities(
            Collection<String> assigneeKeys,
            Map<Long, IdentityUserInfoVO> byId,
            Map<String, IdentityUserInfoVO> byUsername) {
        Map<String, WorkflowAssigneeIdentity> result = new LinkedHashMap<>();
        for (String assigneeKey : assigneeKeys) {
            if (!StringUtils.hasText(assigneeKey)) {
                continue;
            }
            String key = assigneeKey.trim();
            Long userId = parseLong(key);
            IdentityUserInfoVO user = userId == null ? byUsername.get(key) : byId.get(userId);
            if (user != null && user.getUserId() != null) {
                result.put(key, new WorkflowAssigneeIdentity(
                        user.getUserId(), firstText(user.getNickname(), user.getUsername())));
            }
        }
        return result;
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
