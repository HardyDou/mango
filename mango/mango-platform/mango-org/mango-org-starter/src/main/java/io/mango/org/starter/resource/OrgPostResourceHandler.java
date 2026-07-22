package io.mango.org.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceCode;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Resource handler for organization post declarations.
 */
@Component
@RequiredArgsConstructor
public class OrgPostResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "org_post";

    private final PostMapper postMapper;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.ORG_POST);

    @Override
    public String resourceType() {
        return ResourceTypes.ORG_POST;
    }

    @Override
    public String executionTenantField() {
        return "tenantId";
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("postCode")
                .requiredField("postName")
                .fieldDescription("postCode", "岗位编码，租户内幂等键。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        PostEntity post = findByTargetOrBusinessKey(resource);
        boolean creating = post == null;
        LocalDateTime now = LocalDateTime.now();
        if (creating) {
            post = new PostEntity();
            post.setId(fields.longField(resource, "targetId"));
            post.setTenantId(fields.requiredLong(resource, "tenantId"));
            post.setPostCode(fields.requiredString(resource, "postCode"));
            post.setCreateTime(now);
        }
        applyFields(resource, post, now);
        if (creating) {
            post = insertOrReadWinner(resource, post, now);
        } else {
            requireUpdated(resource, postMapper.updateById(post));
        }
        return ResourceSyncResult.of(post.getId(), TARGET_TABLE, "Org post synced: " + post.getPostCode());
    }

    private void applyFields(ResourceDeclaration resource, PostEntity post, LocalDateTime now) {
        post.setPostName(fields.requiredString(resource, "postName"));
        post.setPostSort(fields.intField(resource, "sort", 0));
        post.setPostStatus(statusValue(resource));
        post.setRemark(fields.stringField(resource, "remark"));
        post.setUpdateTime(now);
    }

    private PostEntity insertOrReadWinner(ResourceDeclaration resource, PostEntity candidate, LocalDateTime now) {
        try {
            postMapper.insert(candidate);
            return candidate;
        } catch (DuplicateKeyException exception) {
            PostEntity winner = postMapper.selectByTenantAndCodeForUpdate(
                    fields.requiredLong(resource, "tenantId"),
                    fields.requiredString(resource, "postCode"));
            if (winner == null) {
                throw exception;
            }
            applyFields(resource, winner, now);
            requireUpdated(resource, postMapper.updateById(winner));
            return winner;
        }
    }

    private void requireUpdated(ResourceDeclaration resource, int updatedRows) {
        Require.isTrue(updatedRows == 1, ResourceCode.RESOURCE_CONFLICT,
                "ORG_POST目标在声明租户中不存在或已变化: " + resource.getId());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        PostEntity post = findByTargetOrBusinessKey(resource);
        boolean changed = false;
        if (post != null && !"0".equals(post.getPostStatus())) {
            post.setPostStatus("0");
            post.setUpdateTime(LocalDateTime.now());
            post.setUpdatedAt(LocalDateTime.now());
            int updatedRows = postMapper.updateById(post);
            requireUpdated(resource, updatedRows);
            changed = true;
        }
        Long targetId = null;
        if (post != null) {
            targetId = post.getId();
        }
        return ResourceSyncResult.of(targetId, TARGET_TABLE,
                "Org post disabled: changed=" + changed);
    }

    private PostEntity findByTargetOrBusinessKey(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            PostEntity post = postMapper.selectById(targetId);
            if (post != null) {
                validateTarget(resource, post);
                return post;
            }
        }
        return findByBusinessKey(resource);
    }

    private void validateTarget(ResourceDeclaration resource, PostEntity post) {
        String expectedTenantId = String.valueOf(fields.requiredLong(resource, "tenantId"));
        String expectedPostCode = fields.requiredString(resource, "postCode");
        Require.isTrue(expectedTenantId.equals(post.getTenantId()) && expectedPostCode.equals(post.getPostCode()),
                ResourceCode.RESOURCE_CONFLICT,
                "ORG_POST targetId与声明租户或岗位编码不匹配: " + resource.getId());
    }

    private PostEntity findByBusinessKey(ResourceDeclaration resource) {
        return postMapper.selectOne(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getTenantId, fields.requiredLong(resource, "tenantId"))
                .eq(PostEntity::getPostCode, fields.requiredString(resource, "postCode"))
                .last("LIMIT 1"));
    }

    private String statusValue(ResourceDeclaration resource) {
        String status = fields.stringField(resource, "status");
        if (StringUtils.hasText(status)) {
            return status.trim();
        }
        if (resource.getStatus() == ResourceStatus.DISABLED) {
            return "0";
        }
        return "1";
    }
}
