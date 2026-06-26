package io.mango.org.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
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
        PostEntity post = findByBusinessKey(resource);
        LocalDateTime now = LocalDateTime.now();
        if (post == null) {
            post = new PostEntity();
            post.setTenantId(fields.requiredLong(resource, "tenantId"));
            post.setPostCode(fields.requiredString(resource, "postCode"));
            post.setCreateTime(now);
            post.setCreatedAt(now);
        }
        post.setPostName(fields.requiredString(resource, "postName"));
        post.setPostSort(fields.intField(resource, "sort", 0));
        post.setPostStatus(statusValue(resource));
        post.setRemark(fields.stringField(resource, "remark"));
        post.setUpdateTime(now);
        post.setUpdatedAt(now);
        if (post.getId() == null) {
            postMapper.insert(post);
        } else {
            postMapper.updateById(post);
        }
        return ResourceSyncResult.of(post.getId(), TARGET_TABLE, "Org post synced: " + post.getPostCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        PostEntity post = findByTargetOrBusinessKey(resource);
        boolean changed = false;
        if (post != null && !"0".equals(post.getPostStatus())) {
            post.setPostStatus("0");
            post.setUpdateTime(LocalDateTime.now());
            post.setUpdatedAt(LocalDateTime.now());
            changed = postMapper.updateById(post) > 0;
        }
        return ResourceSyncResult.of(post == null ? null : post.getId(), TARGET_TABLE,
                "Org post disabled: changed=" + changed);
    }

    private PostEntity findByTargetOrBusinessKey(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            PostEntity post = postMapper.selectById(targetId);
            if (post != null) {
                return post;
            }
        }
        return findByBusinessKey(resource);
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
        return resource.getStatus() == ResourceStatus.DISABLED ? "0" : "1";
    }
}
