package io.mango.notice.core.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import org.apache.ibatis.annotations.Param;

@org.apache.ibatis.annotations.Mapper
public interface NoticeBusinessChannelTemplateMapper extends BaseMapper<NoticeBusinessChannelTemplateEntity> {

    /**
     * Reads a registry target before the tenant context can be established.
     * The caller validates the returned tenant and re-reads inside that context.
     */
    @InterceptorIgnore(tenantLine = "true")
    NoticeBusinessChannelTemplateEntity selectRegistryTargetById(@Param("id") Long id);
}
